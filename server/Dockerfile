FROM python:3.12-slim
WORKDIR /app
COPY place_search.py /app/place_search.py
ENV BIND_HOST=0.0.0.0 PORT=8080
USER 65534:65534
EXPOSE 8080
CMD ["python", "place_search.py"]
