docker buildx build --platform linux/amd64 -t samuelwaang/scraper:1.0 . 
docker push samuelwaang/scraper:1.0

# docker pull samuelwaang/scraper:1.0
# docker run -d -p 8080:8080 samuelwaang/scraper:1.0



# cd downloads
# ssh -i scrape-api.pem ec2-user@18.222.129.131
# sudo docker run -d -p 80:8080 samuelwaang/scraper:1.0

# ssh -i scrape-api.pem ec2-user@3.141.31.252