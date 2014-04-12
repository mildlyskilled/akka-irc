package kaning.actors

import scala.collection.mutable.Map
import akka.actor.{PoisonPill, Actor, ActorRef}
import kaning.messages._
import kaning.messages.RegisteredClientList
import kaning.messages.ChatMessage
import kaning.messages.RegisterClientMessage
import kaning.messages.ChatInfo
import kaning.messages.PrivateMessage

class Room extends Actor {
  def receive = usersConnected()

  def usersConnected(connectedClients: Map[String, ActorRef] = Map.empty): Receive = {
    case m @ ChatMessage(x: String) =>
      println(sender.path.name + ": " + x)
      // send this message to everyone in the room except the person who sent it
      connectedClients.values.filter(_ != sender).foreach(_.forward(m))

    case RegisterClientMessage(client: ActorRef, identity: String) =>
      if(connectedClients.contains(identity)){
        println(s"${identity} tried to join AGAIN from ${client}")
        sender ! ChatInfo(s"REGISTRATION FAILED: ${identity} is already registered")
      }else{
        println(s"${identity} joined this channel from ${client}")
        sender ! ChatInfo("REGISTERED SUCCESSFULLY")
        connectedClients.values.filter(_ != sender).foreach(_ ! ChatInfo(s"${identity} join this channel"))
        context.become(usersConnected(connectedClients + ((identity, client))))
      }

    case m @ PrivateMessage(target, _) =>
      connectedClients.values.filter(_.path.name.contains(target)).foreach(_.forward(m))

    case StartUp =>
      println("Received Start Server Signal")
      println(self)

    case RegisteredClients =>
      println(s"${sender.path.name} requested for the room list")
      sender ! RegisteredClientList(connectedClients.keys)

    case Unregister(identity) =>
      println(s"${identity} left this channel")
      // remove client from registered client set and send poison pill
      connectedClients.get(identity).foreach(_ ! PoisonPill) //<-- this is why we use the MUTABLE map
      connectedClients.values.filter(_ != sender).foreach(_ ! ChatInfo(s"${identity} left this channel"))
      context.become(usersConnected(connectedClients - identity))

    case _ => sender ! ChatInfo("Stop mumbling and articulate, you're off protocol buddy")
  }
}
