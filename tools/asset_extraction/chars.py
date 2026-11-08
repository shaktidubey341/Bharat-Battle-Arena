import numpy as np, cv2, os, json
from scipy import ndimage as ndi
OUT="/home/claude/Bharat-Battle-Arena/app/src/main/assets/characters"
ROWS=[(0,190),(190,405),(405,610),(610,860)]
# column splits per layout: (name,row,x0,x1)
L_STD=[("idle",0,360,742),("walk",0,742,1135),("run",0,1135,1540),
       ("jump",1,360,742),("punch",1,742,1140),("kick",1,1140,1540),
       ("block",2,957,1236),("hit",2,1236,1540),
       ("die",3,360,880),("victory",3,1206,1540)]
L_MONK=[("idle",0,0,470),("walk",0,470,1020),("run",0,1020,1540),
        ("jump",1,0,552),("punch",1,552,1100),("kick",1,1100,1540),
        ("block",2,746,1190),("hit",2,1190,1540),
        ("die",3,0,718),("victory",3,1142,1540)]
MANUAL={(2,'run'):[(1140,62,1245,196),(1232,62,1335,196),(1325,62,1432,196),(1415,62,1525,196)]}
NAMES=["monk","neta","kisan","blue","gold"]
def frames_for(im):
    m=(im.max(axis=2)>70).astype(np.uint8)
    m=cv2.morphologyEx(m,cv2.MORPH_OPEN,np.ones((4,4),np.uint8))
    d=cv2.dilate(m,cv2.getStructuringElement(cv2.MORPH_ELLIPSE,(9,9)))
    lab,n=ndi.label(d)
    comps=[]
    for i,sl in enumerate(ndi.find_objects(lab),1):
        y0,y1=sl[0].start,sl[0].stop;x0,x1=sl[1].start,sl[1].stop
        area=int(((lab[sl]==i)&(m[sl]>0)).sum())
        if area>=500: comps.append(dict(i=i,x0=x0,y0=y0,x1=x1,y1=y1,area=area,cx=(x0+x1)/2,cy=(y0+y1)/2))
    changed=True
    while changed:
        changed=False
        for a in range(len(comps)):
            for b in range(a+1,len(comps)):
                A,B=comps[a],comps[b]
                wa,wb=A['x1']-A['x0'],B['x1']-B['x0']
                ha,hb=A['y1']-A['y0'],B['y1']-B['y0']
                if wa/ha>2.4 or wb/hb>2.4: continue
                ov=min(A['x1'],B['x1'])-max(A['x0'],B['x0'])
                gap=max(A['y0'],B['y0'])-min(A['y1'],B['y1'])
                uh=max(A['y1'],B['y1'])-min(A['y0'],B['y0'])
                if ov>0.5*min(wa,wb) and gap<26 and uh<=215 and (A['area']<1500 or B['area']<1500):
                    A['x0']=min(A['x0'],B['x0']);A['x1']=max(A['x1'],B['x1'])
                    A['y0']=min(A['y0'],B['y0']);A['y1']=max(A['y1'],B['y1'])
                    A['area']+=B['area'];A['cx']=(A['x0']+A['x1'])/2;A['cy']=(A['y0']+A['y1'])/2
                    del comps[b];changed=True;break
            if changed:break
    return m,d,lab,comps
def run(pageno,idx,layout,xmin):
    im=cv2.imread("/home/claude/raw/p-%03d.jpg"%(pageno-1))
    m,d,lab,comps=frames_for(im)
    res={}
    for (act,row,xa,xb) in layout:
        ya,yb=ROWS[row]
        sel=[]
        for c in comps:
            w=c['x1']-c['x0'];h=c['y1']-c['y0']
            if not(xa<=c['cx']<xb and ya<=c['cy']<yb and c['x0']>=xmin): continue
            if c['area']<1500 or h<32: continue
            if w/h>2.4 and h<66: continue            # label banners
            if w>420: continue                        # big glow/merged
            sel.append(c)
        sel.sort(key=lambda c:c['cx'])
        stand=[c for c in sel if (c['y1']-c['y0'])>=100 and (c['x1']-c['x0'])<190]
        med_y0=float(np.median([c['y0'] for c in stand])) if stand else 0
        sel=[c for c in sel if c in stand or c['y1']>med_y0+28]
        # split merged wide comps
        if sel:
            hs=sorted(c['y1']-c['y0'] for c in sel);mh=hs[len(hs)//2]
            ws=sorted(c['x1']-c['x0'] for c in sel if (c['y1']-c['y0'])>0.7*mh)
            mw=ws[len(ws)//2] if ws else 80
            fin=[]
            for c in sel:
                w=c['x1']-c['x0']
                k=max(1,int(round(w/ (mw*1.05)))) if w>1.6*mw and act not in("die",) else 1
                if k==1: fin.append((c['x0'],c['y0'],c['x1'],c['y1'],c['i']))
                else:
                    for j in range(k):
                        fin.append((c['x0']+j*w//k,c['y0'],c['x0']+(j+1)*w//k,c['y1'],-1))
            sel=fin
        if (idx,act) in MANUAL:
            sel=[b+(-1,) for b in MANUAL[(idx,act)]]; med_y0=0
        d_out=os.path.join(OUT,"char_%02d"%idx);os.makedirs(d_out,exist_ok=True)
        frs=[]
        for k,(x0,y0,x1,y1,ci) in enumerate(sel):
            pad=4
            X0=max(0,x0-pad);Y0=max(0,y0-pad);X1=min(im.shape[1],x1+pad);Y1=min(im.shape[0],y1+pad)
            crop=im[Y0:Y1,X0:X1].copy()
            mk=(crop.max(axis=2)>45).astype(np.uint8)
            mk=cv2.morphologyEx(mk,cv2.MORPH_OPEN,np.ones((2,2),np.uint8))
            cut=int(med_y0+24-Y0)
            if cut>0:
                l0,n0=ndi.label(cv2.dilate(mk,np.ones((3,3),np.uint8)))
                for q,sl in enumerate(ndi.find_objects(l0),1):
                    if sl[0].stop<=cut: mk[l0==q]=0
            mk=cv2.morphologyEx(mk,cv2.MORPH_CLOSE,cv2.getStructuringElement(cv2.MORPH_ELLIPSE,(5,5)))
            # keep largest components (drop panel border bits)
            l2,n2=ndi.label(mk)
            if n2==0: continue
            areas=ndi.sum(mk,l2,range(1,n2+1))
            keep=np.zeros_like(mk)
            for q,a in enumerate(areas,1):
                if a>0.08*areas.max(): keep|=(l2==q).astype(np.uint8)
            keep=ndi.binary_fill_holes(keep).astype(np.uint8)
            ys,xs=np.where(keep>0)
            ty0,ty1,tx0,tx1=ys.min(),ys.max()+1,xs.min(),xs.max()+1
            a=cv2.GaussianBlur(keep.astype(np.float32),(0,0),0.8)
            a=np.clip((a-0.3)/0.5,0,1)
            rgba=np.dstack([crop,(a*255).astype(np.uint8)])[ty0:ty1,tx0:tx1]
            fn=os.path.join(d_out,"%s_%d.png"%(act,k))
            cv2.imwrite(fn,rgba)
            frs.append(fn)
        res[act]=frs
    return res
if __name__=="__main__":
    meta={}
    for idx,(pg,lay,xmin) in enumerate([(1,L_MONK,0),(2,L_STD,360),(3,L_STD,360),(4,L_STD,360),(5,L_STD,360)],1):
        r=run(pg,idx,lay,xmin)
        meta[idx]={a:len(v) for a,v in r.items()}
        print(idx,NAMES[idx-1],meta[idx])
    json.dump(meta,open("char_meta.json","w"))
