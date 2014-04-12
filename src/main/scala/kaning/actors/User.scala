package kaning.actors

import org.joda.time.DateTime

case class User(identity: String, joinTime: DateTime = DateTime.now())
