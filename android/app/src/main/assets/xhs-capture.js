(() => {
  if (window.__xhsCapture) return;
  const packets = [];
  let seeded = false;
  const domSeen = new Set();
  const matches = url => String(url || '').includes('/api/sns/web/v2/comment/page');
  const push = response => {
    const data = response && response.data;
    if (!Array.isArray(data && data.comments)) return;
    packets.push({
      comments: data.comments.map(comment => ({
        id: String(comment.id || ''),
        pictures: (comment.pictures || []).map(picture => ({
          url: picture.url_pre || picture.urlPre || picture.url_default || picture.urlDefault || picture.url || ''
        }))
      })),
      cursor: String(data.cursor || ''),
      hasMore: !!data.has_more
    });
  };
  const originalFetch = window.fetch;
  window.fetch = function () {
    return originalFetch.apply(this, arguments).then(response => {
      if (matches(response.url)) response.clone().json().then(push).catch(() => {});
      return response;
    });
  };
  const originalOpen = XMLHttpRequest.prototype.open;
  XMLHttpRequest.prototype.open = function (method, url) {
    this.__xhsCommentRequest = matches(url);
    return originalOpen.apply(this, arguments);
  };
  const originalSend = XMLHttpRequest.prototype.send;
  XMLHttpRequest.prototype.send = function () {
    if (this.__xhsCommentRequest) this.addEventListener('load', () => {
      try { push(this.responseType === 'json' ? this.response : JSON.parse(this.responseText)); } catch (_) {}
    });
    return originalSend.apply(this, arguments);
  };
  window.__xhsCapture = {
    drain: () => packets.splice(0),
    snapshot: () => {
      const found = [];
      document.querySelectorAll('.comment-item, .parent-comment').forEach((item, index) => {
        const pictures = [];
        item.querySelectorAll('.comment-picture img, [class*="comment-picture"] img, .comment-picture .img-box').forEach(node => {
          const background = getComputedStyle(node).backgroundImage;
          const url = node.currentSrc || node.src || node.getAttribute('data-src') ||
            (background && background !== 'none' ? background.match(/url\(["']?(.*?)["']?\)/)?.[1] : '');
          if (url && /^https?:/.test(url)) pictures.push({url});
        });
        const id = item.id || item.getAttribute('data-id') || String(index);
        const fresh = pictures.filter(picture => {
          const key = id + ':' + picture.url;
          if (domSeen.has(key)) return false;
          domSeen.add(key);
          return true;
        });
        if (fresh.length) found.push({id: 'dom-' + id + '-' + domSeen.size, pictures: fresh});
      });
      if (found.length) packets.push({comments: found, cursor: '', hasMore: true});
    },
    seed: () => {
      if (seeded || packets.length) return;
      seeded = true;
      const state = window.__INITIAL_STATE__;
      const noteId = location.pathname.match(/[a-f0-9]{24}/i)?.[0];
      const mapRef = state?.note?.noteDetailMap;
      const map = mapRef?._rawValue || mapRef;
      const entry = map && map[noteId];
      const old = state?.noteData?.data?.commentData;
      const comments = entry?.comments || entry?.commentData?.comments || old?.comments;
      if (Array.isArray(comments)) push({data: {comments, cursor: '', has_more: true}});
    },
    scroll: () => {
      const comments = document.querySelectorAll('.comment-item, .parent-comment');
      const last = comments[comments.length - 1];
      if (last) last.scrollIntoView({block: 'end'});
      const candidates = [];
      for (let element = last; element; element = element.parentElement) candidates.push(element);
      candidates.push(document.querySelector('.note-scroller'), document.querySelector('.interaction-container'), document.scrollingElement);
      const target = candidates.find(element => element && element.scrollHeight > element.clientHeight + 30);
      if (!target) return false;
      target.scrollTop = target.scrollHeight;
      target.dispatchEvent(new Event('scroll', {bubbles: true}));
      return true;
    }
  };
})();
