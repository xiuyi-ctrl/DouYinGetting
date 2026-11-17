// 版式探针：把溢出量、越界元素、图片加载情况和各区块高度写进 <title>，
// 便于用 headless Chrome 的 --dump-dom 做无视觉回归检查。
addEventListener('load', () => {
  const sc = document.querySelector('.scroll, .page');
  const imgs = [...document.images];
  const broken = imgs.filter(i => !i.naturalWidth).length;
  const over = sc ? Math.max(0, sc.scrollHeight - sc.clientHeight) : 0;
  const parts = sc
    ? [...sc.children]
        .map(el => `${el.tagName.toLowerCase()}${el.className ? '.' + el.className.trim().split(/\s+/).join('.') : ''}=${Math.round(el.getBoundingClientRect().height)}`)
        .join(' ')
    : '';
  const wide = [...document.querySelectorAll('.screen *')].filter(el => {
    const r = el.getBoundingClientRect();
    return r.width > 0 && (r.right > 390.5 || r.left < -0.5);
  }).length;
  document.title = `probe overflow=${over} wide=${wide} imgs=${imgs.length} broken=${broken} | ${parts}`;
});