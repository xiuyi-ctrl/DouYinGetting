(() => {
    const isDetail = value => {
        try {
            const url = new URL(value, location.href);
            return ['www.douyin.com', 'www-hj.douyin.com'].includes(url.hostname)
                && /^\/aweme\/v1\/web\/aweme\/detail\/?$/.test(url.pathname);
        } catch (_) { return false; }
    };
    const remember = result => {
        const item = result?.aweme_detail;
        const id = location.pathname.match(/^\/(?:video|note)\/(\d+)\/?$/)?.[1];
        if (id && String(item?.aweme_id) === id) {
            const images = item.images?.length ? item.images : item.image_post_info?.images;
            const previous = window.__xiuyiWorkMedia;
            if (previous?.aweme_id === id && (previous.images?.length || 0) > (images?.length || 0)) return;
            const slides = previous?.aweme_id === id && images?.length && previous.images?.length === images.length
                ? images.map((image, index) => {
                    const earlier = previous.images[index];
                    return earlier?.video && !image?.video ? {...image, video: earlier.video} : image;
                }) : images;
            window.__xiuyiWorkMedia = {aweme_id: id, images: slides, video: item.video};
        }
    };

    const fetch = window.fetch;
    window.fetch = function (...args) {
        const request = fetch.apply(this, args);
        if (isDetail(args[0]?.url || args[0])) {
            request.then(response => {
                if (response.ok) return response.clone().json().then(remember);
            }).catch(() => {});
        }
        return request;
    };

    const open = XMLHttpRequest.prototype.open;
    XMLHttpRequest.prototype.open = function (...args) {
        const result = open.apply(this, args);
        if (isDetail(args[1])) {
            this.addEventListener('load', () => {
                try {
                    if (this.status === 200) remember(this.responseType === 'json' ? this.response : JSON.parse(this.responseText));
                } catch (_) { }
            }, {once: true});
        }
        return result;
    };
})();
