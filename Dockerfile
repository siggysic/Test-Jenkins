FROM jetty:9.3.8

ADD target/webapp /var/lib/jetty/webapps/ROOT

EXPOSE 8080

ENTRYPOINT ["/docker-entrypoint.bash"]
CMD ["java","-Djava.io.tmpdir=/tmp/jetty","-Drun.mode=production","-jar","/usr/local/jetty/start.jar"]