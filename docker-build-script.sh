docker buildx build --platform linux/amd64 -t samuelwaang/scraper:1.0 . 
docker push samuelwaang/scraper:1.0

# docker pull samuelwaang/scraper:1.0
# docker run -d -p 8080:8080 samuelwaang/scraper:1.0
