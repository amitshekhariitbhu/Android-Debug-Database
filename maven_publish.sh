#upload to mavenCentral()
#https://s01.oss.sonatype.org/#stagingRepositories
./gradlew :debug-db:publishMavenPublicationToMavenRepository

./gradlew :debug-db-encrypt:publishMavenPublicationToMavenRepository