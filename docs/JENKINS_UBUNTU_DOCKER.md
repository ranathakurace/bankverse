# Jenkins on Ubuntu with Docker

This learning pipeline uses the repository-root `Jenkinsfile`. On an Ubuntu Docker host, it builds a Linux Maven 17 test image, runs the BankVerse backend suite, then runs the Selenium regression against a disposable H2 instance. It never connects to PostgreSQL or needs production credentials.

## Ubuntu prerequisites

Install Docker Engine and Git, then sign out and back in after adding your Linux user to the Docker group:

```bash
sudo apt update
sudo apt install -y docker.io git
sudo systemctl enable --now docker
sudo usermod -aG docker "$USER"
```

## Start Jenkins

For a learning machine, build the supplied Jenkins-controller image. It includes the Docker CLI; Docker itself continues to run on the Ubuntu host. Then start the controller with a persistent volume and access to the host Docker socket. Install the **Docker Pipeline** and **JUnit** plugins from **Manage Jenkins → Plugins**.

```bash
git clone --branch feature/BV-003A-customer-activation https://github.com/ranathakurace/bankverse.git
cd bankverse
docker build -t bankverse-jenkins-controller -f ci/jenkins/Dockerfile.controller .
docker volume create jenkins_home
docker run -d --name bankverse-jenkins --restart unless-stopped \
  -p 8081:8080 -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  --group-add "$(stat -c '%g' /var/run/docker.sock)" \
  bankverse-jenkins-controller
```

Open `http://YOUR_UBUNTU_HOST:8081`. Retrieve the initial password with:

```bash
docker exec bankverse-jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

> The Docker socket gives Jenkins control of Docker on that host. Use this approach only for a learning or isolated CI machine. For a shared environment, use short-lived dedicated agents with restricted permissions instead.

## Create the pipeline job

1. Select **New Item → Pipeline** and name it `bankverse-ci`.
2. Under **Pipeline**, choose **Pipeline script from SCM**.
3. Select **Git**, then use `https://github.com/ranathakurace/bankverse.git`.
4. Set the branch specifier to `*/feature/BV-003A-customer-activation`.
5. Set **Script Path** to `Jenkinsfile`.
6. Save and select **Build Now**.

The job executes two quality gates: backend unit/integration tests and headless Selenium regression. Jenkins publishes JUnit results and retains failure screenshots and application logs.

## Learning sequence

1. Run the job successfully from the current branch.
2. Change one assertion locally, push a new branch, and observe Jenkins fail with evidence.
3. Fix it and push again.
4. Create a GitHub pull request and configure a GitHub webhook to trigger the job on each push.

No deploy stage is included: deployment requires a target environment, approval process, and secrets that should not be invented or committed.
