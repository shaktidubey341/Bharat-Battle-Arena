import numpy as np, cv2
from scipy import ndimage as ndi
def checker_levels(im):
    g=im.mean(axis=2).astype(np.uint8)
    sat=(im.max(axis=2).astype(int)-im.min(axis=2))
    sel=g[sat<14]
    h=np.bincount(sel,minlength=256).astype(float)
    h=np.convolve(h,np.ones(5)/5,'same')
    p1=int(h.argmax()); h2=h.copy(); h2[max(0,p1-14):p1+15]=0
    p2=int(h2.argmax()); return sorted([p1,p2])
def fgmask(im,tol=13,satmax=18):
    lv=checker_levels(im)
    g=im.mean(axis=2)
    sat=(im.max(axis=2).astype(int)-im.min(axis=2))
    bg=np.zeros(g.shape,bool)
    for l in lv: bg|=(np.abs(g-l)<tol)
    bg&=(sat<satmax)
    fg=~bg
    fg=cv2.morphologyEx(fg.astype(np.uint8),cv2.MORPH_OPEN,np.ones((3,3),np.uint8))
    return fg,lv
def cell_sprite(im,fg,box,minarea=150,dil=5,fill=True):
    x0,y0,x1,y1=box
    sub=fg[y0:y1,x0:x1].copy()
    d=cv2.dilate(sub,cv2.getStructuringElement(cv2.MORPH_ELLIPSE,(dil,dil)))
    lab,n=ndi.label(d)
    if n==0: return None
    areas=ndi.sum(sub,lab,range(1,n+1))
    keep=np.zeros_like(sub)
    for q,a in enumerate(areas,1):
        if a>=max(minarea,0.04*areas.max()): keep|=((lab==q)&(sub>0)).astype(np.uint8)
    if fill: keep=ndi.binary_fill_holes(cv2.dilate(keep,np.ones((3,3),np.uint8))).astype(np.uint8)
    ys,xs=np.where(keep>0)
    if len(ys)==0: return None
    a=cv2.GaussianBlur(keep.astype(np.float32),(0,0),0.7);a=np.clip((a-0.35)/0.45,0,1)
    rgb=im[y0:y1,x0:x1]
    rgba=np.dstack([rgb,(a*255).astype(np.uint8)])
    return rgba[ys.min():ys.max()+1, xs.min():xs.max()+1]
