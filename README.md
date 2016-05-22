# This One's for the Girls

This project is built using ClojureScript on Node.js.  The site can be thought of as a static site generator, where
the site generation happens via AWS Lambda.  Here are some useful things to know:

- There are several private lambda functions that are only invoked via the command line, or the AWS console.
  - `set-admin-creds` sets the username and password to log into the admin area on the site. Requires `username` and `password` params.
  - `set-token-secret` sets the secret key used for signing json web tokens that confer permission to use the admin.
  Requires `secret` parameter
  - `generate-all-pages` does what it says - takes the current information available and generates all pages on the site. Takes no
  parameters.

- Deployments are done with the `deploy` script in the root of this project.  It requires `leiningen` and `boot` to be installed,
and the shell user to have credentials for AWS.  It will compile the project, upload the Lambda functions to AWS, and the
static assets to S3. If anything to do with page generation has changed, you'll probably want to run `generate-all-pages`.

- Parts of this site's functionality are handled by these AWS services:
  - Lambda
  - API Gateway
  - S3
  - Cloudfront
  - IAM
  - Certificate Manager

## Developement

- There are fake AWS service components that conform to the same protocols as the production versions
- I use figwheel to autobuild both the backend and frontend code, with `rlwrap lein figwheel lambda-dev admin-dev contact-dev`
- To run the dev server, `node out/lambda-dev/thisonesforthegirls.js` after figwheel builds the first time
- Browser access is at localhost:3449
