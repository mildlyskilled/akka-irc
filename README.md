actor-chat
==========

chat client and server with remote actors. This branch uses the *Akka actors* whereas the master branch uses the built in scala actors from Scala 2.9.x

Thanks to the guys from LSUG for contributions to making it more Scala-esque

### Heroku

The server part of this application can be deployed to [Heroku](http://www.heroku.com). You will need a Heroku account
and the and the [toolbelt](https://toolbelt.heroku.com/) installed. Follow [these instructions](https://devcenter.heroku.com/articles/getting-started-with-scala#deploy-your-application-to-heroku)
on how to deploy the application. Other useful commands include:

`heroku logs -t` - Tails the log<br>
`git push heroku akka-actors:master` - Pushes the latest version to heroku<br>


