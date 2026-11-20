"""Renders the Threads carousel (1080x1350 PNGs) into docs/social/threads-carousel/.

python tools/carousel.py   (needs: pip install playwright && playwright install chromium)
"""
import pathlib
from playwright.sync_api import sync_playwright

ROOT = pathlib.Path(__file__).resolve().parent.parent
ART = (ROOT / "app/src/main/res/drawable-nodpi").as_uri()
FONT = (ROOT / "app/src/main/res/font").as_uri()
SHOT = (ROOT / "docs/screenshots/ru").as_uri()
LOGO = (ROOT / "docs/logo.png").as_uri()
OUT = ROOT / "docs/social/threads-carousel"


def pet(sp, f=2, cls=""):
    return f'<img class="{cls}" src="{ART}/pet_{sp}_{f}.webp">'


def egg(sp, cls=""):
    return f'<img class="{cls}" src="{ART}/egg_{sp}.webp">'


ALL = ["bull", "otter", "lynx", "octopus", "crab", "hedgehog", "kangaroo", "hummingbird", "turtle", "sloth",
       "squirrel", "gorilla", "ant", "owl", "mole", "hamster", "crane", "phoenix", "mammoth", "dragon"]

CSS = f"""
@font-face {{ font-family: N; src: url({FONT}/nunito_bold.ttf); font-weight: 700; }}
@font-face {{ font-family: N; src: url({FONT}/nunito_semibold.ttf); font-weight: 600; }}
@font-face {{ font-family: P; src: url({FONT}/press_start.ttf); }}
* {{ box-sizing: border-box; margin: 0; padding: 0; }}
body {{ background: #333; }}
.s {{ width: 1080px; height: 1350px; position: relative; overflow: hidden; background: #F6EFE6;
     font-family: N; color: #3A2A22; display: flex; flex-direction: column; }}
.top {{ background: #E8603C; color: #fff; padding: 84px 80px 110px; border-radius: 0 0 50% 50% / 0 0 90px 90px; }}
h1 {{ font-size: 82px; line-height: 1.08; font-weight: 700; letter-spacing: -1px; }}
h1 em {{ font-style: normal; color: #FFE2B8; }}
.sub {{ font-size: 40px; font-weight: 600; margin-top: 24px; opacity: .92; line-height: 1.25; }}
.n {{ position: absolute; right: 48px; bottom: 40px; font-family: P; font-size: 20px; color: #C9B7A6; }}
.brand {{ position: absolute; left: 56px; bottom: 36px; font-size: 30px; font-weight: 700; color: #B9A390; }}
.body {{ flex: 1; display: flex; align-items: center; justify-content: center; padding: 0 60px 80px; }}
.card {{ background: #fff; border-radius: 40px; box-shadow: 0 10px 30px #00000014; }}
"""

S1 = f"""<div class="s" style="background:#2B1D17">
 <div style="position:absolute;inset:0;background:url({ART}/gym_bg.webp) center/cover;filter:saturate(1.05)"></div>
 <div style="position:absolute;inset:0;background:linear-gradient(#000000c0 0%,#00000030 38%,#00000000 60%,#000000a0 100%)"></div>
 <div style="position:relative;padding:96px 80px 0;color:#fff">
  <h1 style="font-size:96px">Ты жмёшь —<br>и <em>бычок</em> жмёт<br>вместе с тобой</h1>
 </div>
 {pet("squirrel", 4, "a")}{pet("bull", 3, "b")}{pet("ant", 4, "c")}
 <div class="brand" style="color:#fff;opacity:.85">Pumpling</div><div class="n" style="color:#fff9">1/7</div>
 <style>.a,.b,.c{{position:absolute;filter:drop-shadow(0 18px 14px #0006)}}
 .b{{width:560px;left:260px;bottom:170px}} .a{{width:300px;left:10px;bottom:150px}} .c{{width:300px;right:0;bottom:150px;transform:scaleX(-1)}}</style>
</div>"""

grid = "".join(f'<div class="g">{pet(sp)}</div>' for sp in ALL)
S2 = f"""<div class="s">
 <div class="top"><h1>20 питомцев,<br>и у каждого свой характер</h1>
 <div class="sub">Бычок любит ноги, выдра — кардио, сова — вечерние тренировки</div></div>
 <div class="body"><div style="display:grid;grid-template-columns:repeat(5,1fr);gap:14px 10px;width:100%">{grid}</div></div>
 <div class="brand">Pumpling</div><div class="n">2/7</div>
 <style>.g{{height:162px;display:flex;align-items:center;justify-content:center}} .g img{{max-width:180px;max-height:154px}}</style>
</div>"""

stages = [("egg", "Яйцо"), (0, "Малыш"), (1, "Подросток"), (2, "Взрослый")]
row = "".join(
    f'<div class="st"><div class="pic">{egg("bull") if f == "egg" else pet("bull", f)}</div><div class="lb">{lb}</div></div>'
    + ('<div class="ar">›</div>' if i < 3 else "") for i, (f, lb) in enumerate(stages))
S3 = f"""<div class="s">
 <div class="top"><h1>Каждый подход —<br>это еда</h1>
 <div class="sub">А растут они в дни отдыха,<br>как настоящие мышцы</div></div>
 <div class="body" style="flex-direction:column;gap:56px">
  <div class="card" style="display:flex;align-items:flex-end;justify-content:center;padding:50px 30px 40px;width:100%">{row}</div>
  <div class="card zz">{pet("bull", 5)}<div><b>Бублик спит</b><br><span>а пока растёт 💪</span></div></div>
 </div>
 <div class="brand">Pumpling</div><div class="n">3/7</div>
 <style>.st{{display:flex;flex-direction:column;align-items:center;gap:16px}}
 .pic{{height:280px;display:flex;align-items:flex-end}} .lb{{font-size:30px;font-weight:700;color:#8A7563}}
 .st:nth-child(1) img{{width:130px}} .st:nth-child(3) img{{width:170px}} .st:nth-child(5) img{{width:215px}} .st:nth-child(7) img{{width:270px}}
 .ar{{font-size:70px;color:#E8603C;align-self:center;margin:0 2px 60px}}
 .zz{{display:flex;align-items:center;gap:30px;padding:26px 50px;font-size:40px;width:100%}} .zz img{{width:300px}} .zz span{{color:#8A7563;font-weight:600}}</style>
</div>"""

eggs = [("otter", "6 часов кардио"), ("octopus", "200 подтягиваний"), ("hedgehog", "час планок"),
        ("crab", "120 подходов жимов"), ("gorilla", "25 рекордов"), ("phoenix", "вернуться после перерыва")]
eg = "".join(f'<div class="card e">{egg(sp)}<div>{t}</div></div>' for sp, t in eggs)
S4 = f"""<div class="s">
 <div class="top"><h1>Новые вылупляются<br>из яиц</h1>
 <div class="sub">Какое яйцо найдёшь — зависит от того, как тренируешься</div></div>
 <div class="body"><div style="display:grid;grid-template-columns:repeat(3,1fr);gap:28px;width:100%">{eg}</div></div>
 <div class="brand">Pumpling</div><div class="n">4/7</div>
 <style>.e{{display:flex;flex-direction:column;align-items:center;gap:18px;padding:34px 16px 30px;font-size:31px;font-weight:700;text-align:center;line-height:1.15}}
 .e img{{height:170px}} .e:first-child{{outline:6px solid #E8603C}}</style>
</div>"""

forms = [("gorilla", "Силач", "вырос на железе"), ("turtle", "Марафонец", "вырос на кардио"),
         ("kangaroo", "Атлет", "вырос на прыжках"), ("lynx", "Гимнаст", "вырос на растяжке")]
fm = "".join(f'<div class="card f">{pet(sp)}<b>{n}</b><span>{t}</span></div>' for sp, n, t in forms)
S5 = f"""<div class="s">
 <div class="top" style="padding-bottom:90px"><h1>Взрослые<br>похожи на тебя</h1>
 <div class="sub">Телосложение зависит от того, на чём их растили</div></div>
 <div class="body"><div style="display:grid;grid-template-columns:1fr 1fr;gap:28px;width:100%">{fm}</div></div>
 <div class="brand">Pumpling</div><div class="n">5/7</div>
 <style>.f{{display:flex;flex-direction:column;align-items:center;padding:26px 20px 26px}} .f img{{height:180px;margin-bottom:10px}}
 .f b{{font-size:44px}} .f span{{font-size:30px;color:#8A7563;font-weight:600}}</style>
</div>"""

S6 = f"""<div class="s">
 <div class="top" style="padding-bottom:90px"><h1>И нормальный<br>дневник тренировок</h1>
 <div class="sub">275 упражнений · 33 программы · статистика</div></div>
 <div class="phones"><img src="{SHOT}/2-log.png"><img src="{SHOT}/4-stats.png"></div>
 <div class="card pill">Пропустил день? Не страшно —<br>считается вся неделя</div>
 <div class="n">6/7</div>
 <style>.phones{{display:flex;justify-content:center;gap:36px;margin-top:50px}}
 .phones img{{width:400px;height:720px;object-fit:cover;object-position:top;border-radius:36px;border:10px solid #3A2A22;box-shadow:0 20px 40px #0003}}
 .phones img:first-child{{transform:rotate(-3deg)}} .phones img:last-child{{transform:rotate(3deg)}}
 .pill{{position:absolute;left:80px;right:80px;bottom:70px;padding:30px 40px;font-size:40px;font-weight:700;text-align:center;line-height:1.2}}</style>
</div>"""

S7 = f"""<div class="s" style="background:#E8603C;color:#fff;align-items:center;justify-content:center;text-align:center">
 <img src="{LOGO}" style="width:210px;border-radius:48px;box-shadow:0 14px 30px #0003">
 <div style="font-size:110px;font-weight:700;margin-top:30px;letter-spacing:-2px">Pumpling</div>
 <div style="font-size:46px;font-weight:600;margin-top:10px;line-height:1.35">Бесплатно<br>Без рекламы и регистрации<br>Android</div>
 <div class="card" style="margin-top:64px;padding:26px 54px;color:#E8603C;font-size:44px;font-weight:700">Ссылка в комментах 👇</div>
 {pet("otter", 4, "o")}{pet("bull", 4, "u")}
 <div class="n" style="color:#fff9">7/7</div>
 <style>.o,.u{{position:absolute;bottom:40px;width:250px}} .o{{left:30px}} .u{{right:30px;transform:scaleX(-1)}}</style>
</div>"""

SLIDES = [S1, S2, S3, S4, S5, S6, S7]

OUT.mkdir(parents=True, exist_ok=True)
html = OUT / "_slides.html"
html.write_text(f"<!doctype html><meta charset=utf-8><style>{CSS}</style>" + "".join(SLIDES), encoding="utf-8")
with sync_playwright() as p:
    b = p.chromium.launch()
    pg = b.new_page(viewport={"width": 1080, "height": 1350})
    pg.goto(html.as_uri()); pg.wait_for_load_state("networkidle"); pg.evaluate("document.fonts.ready")
    for i, el in enumerate(pg.query_selector_all(".s"), 1):
        el.screenshot(path=str(OUT / f"{i}.png"))
    b.close()
html.unlink()
print("done:", OUT)
