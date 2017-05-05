#!groovy
timestamps {
    node {
        try {
            checkout scm
			sh 'git rev-parse HEAD > commit'
			def gitRevision = readFile('commit').trim()
			echo "Revision: ${gitRevision}" 
            docker.image('maven:3.3.3-jdk-8').withRun('-it -v /var/lib/m2:/root/.m2', 'bash') {c ->
                sh """
                docker cp . ${c.id}:/root/netphony-topology
                docker exec ${c.id} mvn -f /root/netphony-topology package -P generate-full-jar
                docker cp ${c.id}:/root/netphony-topology/target .
                """
            }
            def tadsVersion = version() + ".${env.BUILD_NUMBER}"
			buildImage("tads:${tadsVersion}", "-f Dockerfile.tads --build-arg GIT_REVISION=${gitRevision} .")
        } catch (any) {
			currentBuild.result = 'FAILURE'
			throw any
		} finally {
			step([$class: 'Mailer', recipients: '5gex-devel@tmit.bme.hu'])
		}
    }
}

def version() {
  def matcher = readFile('pom.xml') =~ '<version>(.+)</version>'
  matcher ? matcher[0][1] : null
}

def buildImage(String tag, String args = '.') {
    docker.withRegistry('https://5gex.tmit.bme.hu') {
        def image = docker.build(tag, args)
        image.push('unstable')
    }
}