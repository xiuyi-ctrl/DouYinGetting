from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageFilter
import math

OUT = Path(__file__).parent
W, H = 900, 1950
FONT = r'C:\Windows\Fonts\msyh.ttc'
BOLD = r'C:\Windows\Fonts\msyhbd.ttc'

ink = '#1D343A'
muted = '#76878A'
green = '#159B80'
blue = '#5E69E8'
coral = '#F37D6A'
bg = '#F7F8F8'

def f(size, bold=False):
    return ImageFont.truetype(BOLD if bold else FONT, size)

def text(d, xy, value, size, fill=ink, bold=False, anchor=None):
    d.text(xy, value, font=f(size, bold), fill=fill, anchor=anchor)

def rr(d, box, radius, fill, outline=None, width=1):
    d.rounded_rectangle(box, radius=radius, fill=fill, outline=outline, width=width)

def shadow(im, box, radius=32, blur=18, alpha=30, dy=8):
    layer = Image.new('RGBA', im.size)
    ld = ImageDraw.Draw(layer)
    ld.rounded_rectangle((box[0], box[1]+dy, box[2], box[3]+dy), radius, fill=(28, 45, 52, alpha))
    im.alpha_composite(layer.filter(ImageFilter.GaussianBlur(blur)))

def pill(d, box, fill, label, size, color, bold=False):
    rr(d, box, (box[3]-box[1])//2, fill)
    text(d, ((box[0]+box[2])/2, (box[1]+box[3])/2), label, size, color, bold, 'mm')

def star(d, x, y, r, color):
    points=[]
    for i in range(10):
        a=math.pi*i/5-math.pi/2
        rad=r if i%2==0 else r*.45
        points.append((x+math.cos(a)*rad,y+math.sin(a)*rad))
    d.polygon(points, fill=color)

def icon_sheet(d, x, y, color=green, scale=1):
    s=scale
    rr(d, (x,y,x+138*s,y+148*s), 28*s, color)
    d.polygon([(x+100*s,y),(x+138*s,y+38*s),(x+105*s,y+37*s)], fill='#B5F1D6')
    d.arc((x+27*s,y+43*s,x+62*s,y+78*s), 195, 345, fill=ink, width=max(2,int(6*s)))
    d.arc((x+76*s,y+43*s,x+111*s,y+78*s), 195, 345, fill=ink, width=max(2,int(6*s)))
    d.arc((x+43*s,y+63*s,x+95*s,y+119*s), 0, 180, fill=ink, width=max(2,int(6*s)))

def icon_douyin(d, cx, cy, color='#EC6570'):
    d.line([(cx+14,cy-48),(cx+14,cy+24),(cx-6,cy+42)], fill=color, width=17, joint='curve')
    d.line([(cx+14,cy-48),(cx+38,cy-30),(cx+52,cy-26)], fill=color, width=17, joint='curve')
    d.ellipse((cx-43,cy+10,cx+5,cy+53), outline=color, width=16)

def icon_book(d, cx, cy, color='#E8737B'):
    rr(d,(cx-65,cy-46,cx+65,cy+45),16,'#FFF8F2',outline=color,width=7)
    d.line((cx,cy-40,cx,cy+40),fill=color,width=5)
    d.line((cx-46,cy-21,cx-17,cy-21),fill=color,width=5)
    d.line((cx+17,cy-21,cx+46,cy-21),fill=color,width=5)
    star(d,cx+45,cy+48,15,'#F9BF63')

def status(d):
    text(d,(50,40),'9:41',29,ink,True)
    d.arc((748,42,780,68),195,345,fill=ink,width=5)
    d.arc((755,47,773,64),195,345,fill=ink,width=4)
    d.ellipse((763,64,769,70),fill=ink)
    for i,h in enumerate((12,17,22,27)):
        rr(d,(796+i*10,70-h,802+i*10,70),2,ink)
    rr(d,(846,43,884,70),7,None,ink,3)
    rr(d,(850,47,872,66),3,ink)
    rr(d,(885,51,890,62),2,ink)

def home():
    im=Image.new('RGBA',(W,H),bg)
    d=ImageDraw.Draw(im)
    d.rectangle((0,0,W,510),fill='#FFFFFF')
    status(d)
    # Hero collage, echoing the reference's large illustrated header.
    d.ellipse((-60,88,400,486),fill='#EBEDFF')
    d.ellipse((185,114,443,382),fill='#6671E9')
    d.ellipse((20,246,289,520),fill='#6671E9')
    d.polygon([(0,366),(230,232),(425,332),(339,512),(0,512)],fill='#6671E9')
    d.ellipse((11,137,37,162),fill='#F7C76F')
    d.ellipse((385,195,407,217),fill='#F7C76F')
    star(d,84,142,22,'#FFCC72'); star(d,348,154,17,'#FFCC72'); star(d,317,346,12,'#FFFFFF')
    # photo card and sticker mascot
    shadow(im,(96,231,334,430),25,10,25)
    d=ImageDraw.Draw(im)
    rr(d,(96,231,334,430),25,'#FFF9F0')
    rr(d,(116,250,313,381),18,'#FFC9B9')
    d.ellipse((145,281,208,343),fill='#E9788A')
    d.ellipse((197,263,278,345),fill='#FDBA78')
    d.polygon([(118,380),(188,311),(237,354),(271,330),(314,382)],fill='#A6D9C4')
    rr(d,(178,178,325,333),27,'#F3F9E8')
    icon_sheet(d,184,188,'#5FD6A1',.91)
    # chip and wordmark
    pill(d,(425,141,688,195),'#EEF8F2','●  评论表情收藏夹',24,green,True)
    text(d,(427,226),'咻咿',80,blue,True)
    text(d,(635,233),'提取',66,ink,True)
    text(d,(430,354),'抖音 / 小红书评论素材提取',27,muted)
    text(d,(430,403),'粘贴链接，预览后再保存',25,muted)
    # feature title
    text(d,(44,548),'选择功能',42,ink,True)
    text(d,(44,607),'从你想读取的平台开始',25,muted)
    d.line((44,668,856,668),fill='#E7EAEA',width=2)
    # Two feature cards
    shadow(im,(44,704,427,1133),36,14,22)
    shadow(im,(473,704,856,1133),36,14,22)
    d=ImageDraw.Draw(im)
    rr(d,(44,704,427,1133),36,'#E8E9FF')
    rr(d,(473,704,856,1133),36,'#FFEDE9')
    rr(d,(82,743,241,895),34,'#D3D8FF')
    rr(d,(511,743,670,895),34,'#FFD8D2')
    icon_douyin(d,160,816,'#5D66D9')
    icon_book(d,590,816,'#E97879')
    star(d,344,771,18,'#F9C76A')
    d.ellipse((785,773,801,789),fill='#F9B994')
    text(d,(82,926),'抖音链接读取',40,ink,True)
    text(d,(82,985),'表情包 · 图片 · GIF',25,muted)
    text(d,(511,926),'小红书链接读取',36,ink,True)
    text(d,(511,985),'图片 · 回复 · 实况',25,muted)
    pill(d,(305,1050,391,1100),'#FFFFFF','→',34,blue,True)
    pill(d,(734,1050,820,1100),'#FFFFFF','→',34,coral,True)
    # dock
    shadow(im,(0,1770,900,1950),40,18,18)
    d=ImageDraw.Draw(im)
    rr(d,(0,1770,900,1975),43,'#FFFFFF')
    d.ellipse((373,1717,527,1871),fill=blue)
    d.polygon([(420,1790),(450,1765),(480,1790),(474,1790),(474,1824),(426,1824),(426,1790)],fill='#FFFFFF')
    text(d,(150,1860),'抖音',26,muted,True,'mm')
    text(d,(450,1900),'首页',25,blue,True,'mm')
    text(d,(750,1860),'小红书',26,muted,True,'mm')
    d.ellipse((138,1804,162,1828),outline=muted,width=5)
    d.line((151,1804,151,1791),fill=muted,width=5)
    d.line((730,1816,769,1816),fill=muted,width=5)
    d.line((730,1804,769,1804),fill=muted,width=5)
    im.convert('RGB').save(OUT/'xiuyi-home-concept.png',quality=95)

def thumb(d,box,variant):
    x1,y1,x2,y2=box
    rr(d,box,18,('#B7DCE1','#BFD0F1','#F4CFC0','#C9DDBE')[variant%4])
    d.ellipse((x1+18,y1+20,x1+88,y1+90),fill=('#F9D8A7','#D7B5DB','#FFF3D4','#FBDB9A')[variant%4])
    d.polygon([(x1+8,y2-18),(x1+75,y1+66),(x1+120,y2-44),(x2-7,y1+48),(x2-7,y2-7),(x1+8,y2-7)],fill=('#559E8A','#6779C9','#C38386','#7AA286')[variant%4])

def reader():
    im=Image.new('RGBA',(W,H),bg)
    d=ImageDraw.Draw(im)
    status(d)
    text(d,(48,126),'‹',72,ink)
    text(d,(144,146),'小红书链接读取',40,ink,True)
    pill(d,(696,125,855,185),'#EAF6F1','扫描记录',23,green,True)
    shadow(im,(43,223,856,524),32,14,18)
    d=ImageDraw.Draw(im)
    rr(d,(43,223,856,524),32,'#FFFFFF')
    text(d,(80,264),'笔记链接',29,ink,True)
    pill(d,(700,251,817,303),'#E8F7EE','网页登录',19,green,True)
    rr(d,(80,326,817,397),18,'#F4F7F6')
    text(d,(103,346),'https://xhslink.cn/o/45KyBOBwan0',25,ink)
    rr(d,(80,418,817,490),18,green)
    text(d,(449,454),'打开作品',29,'#FFFFFF',True,'mm')
    rr(d,(43,554,856,751),31,'#E8F4F0')
    text(d,(81,587),'扫描中 · 已发现 12 个素材',28,ink,True)
    text(d,(81,636),'主评论 8  ·  回复 3  ·  合计 11',24,green)
    rr(d,(81,693,818,705),6,'#CBE8DD')
    rr(d,(81,693,558,705),6,green)
    text(d,(43,791),'网页操作',25,green,True)
    d.line((804,784,822,801),fill=green,width=5)
    d.line((822,801,840,784),fill=green,width=5)
    text(d,(43,855),'提取结果',42,ink,True)
    text(d,(43,921),'图片、GIF 与视频自动显示在这里',24,muted)
    pill(d,(43,976,190,1034),ink,'全部 12',24,'#FFFFFF',True)
    pill(d,(207,976,359,1034),'#E9EFEC','动态 3',24,muted,True)
    pill(d,(375,976,522,1034),'#E9EFEC','图片 9',24,muted,True)
    # preview cards, 3 columns
    for i in range(6):
        col,row=i%3,i//3
        x=43+col*277; y=1076+row*312
        shadow(im,(x,y,x+254,y+287),24,8,14)
        d=ImageDraw.Draw(im)
        rr(d,(x,y,x+254,y+287),24,'#FFFFFF')
        thumb(d,(x+9,y+9,x+245,y+222),i)
        tag=('LIVE','图片','视频','GIF','图片','图片')[i]
        tagfill=coral if tag in ('LIVE','视频','GIF') else ink
        pill(d,(x+153,y+21,x+233,y+57),tagfill,tag,17,'#FFFFFF',True)
        if tag in ('LIVE','视频'):
            d.ellipse((x+91,y+77,x+162,y+148),fill='#FFFFFF')
            d.polygon([(x+119,y+95),(x+119,y+131),(x+146,y+113)],fill=tagfill)
        text(d,(x+22,y+242),'评论'+('实况' if tag=='LIVE' else tag),22,ink)
    rr(d,(0,1730,900,1960),40,'#FFFFFF')
    text(d,(44,1803),'已提取 12 个',28,ink,True)
    rr(d,(544,1772,855,1864),24,ink)
    text(d,(700,1817),'保存全部',30,'#FFFFFF',True,'mm')
    text(d,(44,1891),'美术示意 · 数量与缩略图为演示数据',18,muted)
    im.convert('RGB').save(OUT/'xiuyi-reader-concept.png',quality=95)

home()
reader()
