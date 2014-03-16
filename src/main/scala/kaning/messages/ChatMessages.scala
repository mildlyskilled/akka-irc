package kaning.messages

import akka.actor.{ActorRef}
import kaning.actors._
import collection.mutable.Map

sealed trait  Message

case object StartUp extends Message
case object RegisteredClients extends Message

case class Unregister(identity: String) extends Message
case class ChatMessage(msg: String) extends Message
case class PrivateMessage(target: String, msg: String) extends Message
case class ChatInfo(inf: String) extends Message
case class RegisterClientMessage(client:ActorRef, identity:String) extends Message
case class RegisteredClientList(list: Iterable[String]) extends Message
