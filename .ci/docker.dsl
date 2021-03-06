// vim: set filetype=groovy:

def jobName = 'zeebe-DISTRO-docker'
def repository = 'zeebe'
def gitBranch = 'master'

def dockerHubUpload =
'''\
#!/bin/bash -xeu

VERSION=${RELEASE_VERSION}

if [ "${RELEASE_VERSION}" = "SNAPSHOT" ]; then
    VERSION=$(curl -sL https://raw.githubusercontent.com/zeebe-io/zeebe/master/pom.xml | grep '<version>' | head -n 1 | cut -d '>' -f 2 | cut -d '<' -f 1  )
fi

echo "Downloading Zeebe distribution ${VERSION}."
curl -sL "https://app.camunda.com/nexus/service/local/artifact/maven/redirect?r=public&g=io.zeebe&a=zeebe-distribution&v=${VERSION}&p=tar.gz" > zeebe.tar.gz

echo "Building Zeebe Docker image ${RELEASE_VERSION}."
docker build --no-cache -t camunda/zeebe:${RELEASE_VERSION} --build-arg DISTBALL=zeebe.tar.gz .

echo "Authenticating with DockerHub and pushing image."
docker login --username ${DOCKER_HUB_USERNAME} --password ${DOCKER_HUB_PASSWORD} --email ci@camunda.com

docker push camunda/zeebe:${RELEASE_VERSION}

if [ "${IS_LATEST}" = "true" ]; then
    # to make sure we can tag latest, there were problems before
    docker rmi camunda/zeebe:latest

    docker tag -f camunda/zeebe:${RELEASE_VERSION} camunda/zeebe:latest
    docker push camunda/zeebe:latest
fi
'''

freeStyleJob(jobName)
{
    scm
    {
        git
        {
            remote
            {
                github 'zeebe-io/' + repository, 'ssh'
                credentials 'camunda-jenkins-github-ssh'
            }
            branch gitBranch
            extensions
            {
                localBranch gitBranch
                pathRestriction {
                    includedRegions ''
                    excludedRegions 'docs/.*\n\\.ci/.*'
                }
            }
        }
    }

    label 'dind'

    triggers {
        cron('@midnight')
    }

    parameters
    {
        stringParam('RELEASE_VERSION', 'SNAPSHOT', 'Docker image tag')
        booleanParam('IS_LATEST', false, 'If <strong>TRUE</strong> the image is also tagged with latest')
    }

    wrappers
    {
        timestamps()

        timeout
        {
            absolute 10
        }

        credentialsBinding {
          usernamePassword('DOCKER_HUB_USERNAME', 'DOCKER_HUB_PASSWORD', 'camundajenkins-dockerhub')
        }
    }

    steps {
        shell dockerHubUpload
    }

    publishers
    {

        extendedEmail
        {
          triggers
          {
              firstFailure
              {
                  sendTo
                  {
                      culprits()
                  }
              }
              fixed
              {
                  sendTo
                  {
                      culprits()
                  }
              }
          }
        }

    }

    blockOnUpstreamProjects()
    logRotator(-1, 5, -1, 1)

}
