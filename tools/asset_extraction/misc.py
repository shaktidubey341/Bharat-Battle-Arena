import cv2,numpy as np,sys,os
sys.path.insert(0,'/home/claude/work')
from cutout import *
from scipy import ndimage as ndi
A='/home/claude/Bharat-Battle-Arena/app/src/main/assets'
def rd(p): return cv2.imread('/home/claude/raw/p-%03d.jpg'%(p-1))
def save(rel,img):
    fn=os.path.join(A,rel);os.makedirs(os.path.dirname(fn),exist_ok=True);cv2.imwrite(fn,img);return img
def glow_alpha(im,box,smin=70,vmin=235,dil=3):
    x0,y0,x1,y1=box;c=im[y0:y1,x0:x1]
    hsv=cv2.cvtColor(c,cv2.COLOR_BGR2HSV)
    m=((hsv[...,1]>smin)|(hsv[...,2]>vmin)&(hsv[...,1]>20)).astype(np.uint8)
    m=cv2.morphologyEx(m,cv2.MORPH_OPEN,np.ones((3,3),np.uint8))
    m=cv2.dilate(m,np.ones((dil,dil),np.uint8))
    lab,n=ndi.label(m)
    if n==0: return np.dstack([c,np.zeros(c.shape[:2],np.uint8)])
    ar=ndi.sum(m,lab,range(1,n+1));keep=np.isin(lab,[i+1 for i,a in enumerate(ar) if a>0.05*ar.max()])
    keep=ndi.binary_fill_holes(keep).astype(np.float32)
    a=cv2.GaussianBlur(keep,(0,0),1.5)
    return np.dstack([c,(np.clip(a*1.1,0,1)*255).astype(np.uint8)])
def cellbox(W,H,cols,rows,c,r,inset=0,vfrac=1.0):
    cw,ch=W/cols,H/rows
    return (int(c*cw+inset),int(r*ch+inset),int((c+1)*cw-inset),int(r*ch+ch*vfrac-inset))
def sprite(pg,box,tol=13,sat=22,minarea=150,dil=5,name=None):
    im=rd(pg);fg,lv=fgmask(im,tol=tol,satmax=sat)
    s=cell_sprite(im,fg,box,minarea=minarea,dil=dil)
    if name: save(name,s)
    return s
# --- weapons already done
# bullet (black bg): alpha from brightness
im=rd(27);H,W=im.shape[:2]
for k,(c,r) in enumerate([(0,0),(2,0),(3,0),(0,1)]):
    x0,y0,x1,y1=cellbox(W,H,5,2,c,r); y0=int(H*0.33) if r==0 else int(H*0.52); y1=y0+int(H*0.17)
    cr=im[y0:y1,x0:x1].astype(np.float32);a=np.clip(cr.max(axis=2)/160.0,0,1)
    ys,xs=np.where(a>0.15);cr=cr[ys.min():ys.max()+1,xs.min():xs.max()+1];a=a[ys.min():ys.max()+1,xs.min():xs.max()+1]
    save(f'bullets/bullet_{k}.png',np.dstack([cr,a*255]).astype(np.uint8))
# muzzle flash
im=rd(26);H,W=im.shape[:2]
for nm,(c,r) in {'muzzle_flash_big':(2,0),'muzzle_flash_small':(1,0),'spark_burst':(0,1)}.items():
    b=cellbox(W,H,4,2,c,r,inset=4,vfrac=0.75)
    save(f'effects/muzzle_flash/{nm}.png',trim(glow_alpha(im,b,smin=60,vmin=240)) if 'trim' in globals() else glow_alpha(im,b,smin=60,vmin=240))
# grenades
sprite(22,cellbox(1408,768,4,2,0,0,vfrac=1),name='grenades/explosive/grenade.png')
sprite(23,cellbox(1408,768,5,2,0,0),name='grenades/flash/grenade.png')
sprite(24,cellbox(1408,768,5,2,0,0),name='grenades/smoke/grenade.png')
im=rd(23);save('grenades/flash/flash_burst.png',glow_alpha(im,cellbox(1408,768,5,2,4,0,inset=4),smin=0,vmin=225))
# explosion frames (page 29, 5x3)
im=rd(29);H,W=im.shape[:2]
seq=[(1,0),(2,0),(3,0),(4,0),(0,1),(1,1)]
for k,(c,r) in enumerate(seq):
    b=cellbox(W,H,5,3,c,r,inset=3)
    g=glow_alpha(im,b,smin=75,vmin=235)
    save(f'effects/explosion/explosion_{k}.png',g)
# mine (page 25)
for nm,(c,r) in {'mine_placed':(1,0),'mine_armed':(2,0),'mine_warning':(3,0)}.items():
    sprite(25,cellbox(1408,768,4,2,c,r,inset=10,vfrac=0.78),name=f'grenades/mine/{nm}.png',tol=14,sat=22)
# crates / barrel
S=1024/834.0
def sheet(x,y): return int((x-45)*S),int((y-236)*S)
sprite(34,(40,30,235,210),name='props/crate_intact.png')
sprite(34,(300,440,480,600),name='props/crate_damaged.png')
sprite(34,(60,640,230,800),name='props/crate_broken.png')
sprite(36,(8,20,165,200),name='props/barrel.png',tol=16)
# pickups
sprite(38,(60,10,200,140),name='pickups/health.png')
sprite(44,(0,30,150,140),name='pickups/ammo.png')
sprite(51,(170,50,265,175),name='pickups/grenade.png')
sprite(52,(200,30,300,115),name='pickups/mine.png')
# UI buttons (page 53) circles
im=rd(53)
btn={'shoot':(759,478),'run':(932,478),'jet':(1100,478),'reload':(1266,478),'grenade':(759,642),'switch':(932,642),'crouch':(1100,642),'melee':(1266,642),'stick_base':(165,576),'stick_base2':(462,576)}
for n,(cx,cy) in btn.items():
    r=70 if 'stick' not in n else 125
    cr=im[cy-r:cy+r,cx-r:cx+r].copy()
    mk=np.zeros(cr.shape[:2],np.uint8);cv2.circle(mk,(r,r),r-3,255,-1);mk=cv2.GaussianBlur(mk,(0,0),1.2)
    save(f'ui/{n}.png',np.dstack([cr,mk]))
