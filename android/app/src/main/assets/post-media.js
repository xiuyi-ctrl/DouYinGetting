(() => {
    const xhs = location.hostname === 'www.xiaohongshu.com';
    const match = location.pathname.match(xhs
        ? /^\/(?:explore|discovery\/item)\/([a-f\d]{24})\/?$/i
        : /^\/(?:video|note)\/(\d+)\/?$/);
    if (!match) return null;

    const id = match[1];
    const images = [];
    const videos = [];
    const secureUrls = values => [...new Set(values.filter(value => typeof value === 'string')
        .map(value => value.startsWith('//') ? 'https:' + value : value.replace(/^http:/, 'https:'))
        .filter(value => value.startsWith('https://')))];
    const addImage = variants => {
        const urls = secureUrls(variants);
        if (urls.length) images.push(urls);
    };
    const addVideo = variants => videos.push(...secureUrls(variants));

    if (xhs) {
        const entry = window.__INITIAL_STATE__?.note?.noteDetailMap?.[id];
        const note = entry?.note;
        const streams = note?.video?.media?.stream;
        if (streams) {
            for (const stream of [...(streams.h264 || []), ...(streams.h265 || []), ...(streams.av1 || [])]) {
                addVideo([stream.masterUrl, ...(stream.backupUrls || [])]);
            }
        }
        if (note?.type !== 'video' && note?.video == null) {
            for (const image of note?.imageList || []) {
                addImage([image.urlDefault, ...(image.infoList || []).map(info => info.url), image.url, image.urlPre]);
            }
        }
        if (!images.length && !videos.length) {
            const video = document.querySelector('#noteContainer video');
            addVideo([video?.currentSrc, video?.src]);
        }
    } else {
        const player = document.querySelector('[data-e2e="note-detail"] [data-e2e="player-container"]');
        const roots = [window.__xiuyiWorkMedia, window._ROUTER_DATA, window.__INITIAL_STATE__];
        if (player) {
            // The desktop carousel can mount only the active slide. The player receives the full work.
            const fiberKey = Object.keys(player).find(key => key.startsWith('__reactFiber$'));
            for (let fiber = player[fiberKey], depth = 0; fiber && depth < 20; fiber = fiber.return, depth++) {
                const info = fiber.memoizedProps?.awemeInfo;
                if (String(info?.awemeId ?? info?.aweme_id) === id) roots.push(info);
            }
        }
        const rendered = document.getElementById('RENDER_DATA')?.textContent;
        if (rendered) {
            try { roots.push(JSON.parse(decodeURIComponent(rendered))); } catch (_) { }
        }
        let item;
        let bestGallery = [];
        const addresses = value => typeof value === 'string' ? [value] : value?.urlList || value?.url_list || [];
        const seen = new WeakSet();
        for (let i = 0; i < roots.length && i < 15000; i++) {
            const value = roots[i];
            if (!value || typeof value !== 'object' || seen.has(value)) continue;
            seen.add(value);
            if (String(value.awemeId ?? value.aweme_id) === id) {
                if (value.video && !item) item = value;
                const gallery = value.images?.length ? value.images : value.imagePostInfo?.images || value.image_post_info?.images;
                const urls = Array.isArray(gallery) ? gallery.map(image => secureUrls([
                    ...addresses(image), ...addresses(image?.displayImage || image?.display_image)
                ])).filter(variants => variants.length) : [];
                if (urls.length > bestGallery.length) bestGallery = urls;
            }
            for (const child of Object.values(value)) {
                if (child && typeof child === 'object') roots.push(child);
            }
        }
        images.push(...bestGallery);
        if (!images.length) {
            const video = item?.video;
            addVideo([...addresses(video?.playAddrH264 || video?.play_addr_h264),
                ...addresses(video?.playAddr || video?.play_addr),
                ...addresses(video?.downloadAddr || video?.download_addr)]);
        }
        if (!images.length && !videos.length && location.pathname.startsWith('/note/')) {
            for (const image of player?.querySelectorAll('.swiper-slide img') || []) {
                addImage([image.currentSrc, image.src]);
            }
        }
        if (!images.length && !videos.length && location.pathname.startsWith('/video/')) {
            const video = document.querySelector('[data-e2e="feed-active-video"] video, [data-e2e="player-container"] video');
            addVideo([video?.currentSrc, video?.src]);
        }
    }

    // An explicit video metadata URL belongs to the document, unlike arbitrary videos in the feed.
    if (!images.length && !videos.length) {
        addVideo(['og:video:secure_url', 'og:video:url', 'og:video'].map(property =>
            document.querySelector('meta[property="' + property + '"]')?.content));
    }
    return { id, images, videos: secureUrls(videos) };
})();
