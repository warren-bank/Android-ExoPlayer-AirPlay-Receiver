#### Usage:

1. download 10x .ts video segment files:
   ```bash
     call ./TEST_CASE/.bin/1-download-ts/dl.bat
   ```
2. run web server:
   ```bash
     call ./TEST_CASE/.bin/2-serve/httpd.bat
   ```
3. open master manifest in _ExoAirPlayer_:
   ```bash
     airplay_ip='192.168.1.100:8192'
     video_url='http://192.168.1.101:80/master.m3u8'

     curl --silent -X POST \
       -H "Content-Type: text/parameters" \
       --data-binary "Content-Location: ${video_url}\nUse-Cache: true" \
       "http://${airplay_ip}/play"
   ```
4. open httpd requests log in a text editor:
   ```bash
     leafpad ./TEST_CASE/.bin/2-serve/httpd.log
   ```
   - this will show a chronological log of all requests by _ExoAirPlayer_ for both:
     * video manifests (.m3u8)
     * video segments (.ts)
   - the web server uses rewrite rules that:
     * allow the inbound requests to clearly specify the stream resolution for the target file
     * serves the same set of video segments for all stream resolutions
     * examples:
       - video manifest for particular stream resolution
         * URL path: `/480p/video.m3u8`
         * filepath: `/480p.m3u8`
       - video segment for particular stream resolution
         * URL path: `/480p/01.ts`
         * filepath: `/video/01.ts`
