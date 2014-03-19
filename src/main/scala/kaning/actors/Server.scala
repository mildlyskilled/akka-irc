package kaning.actors

import akka.actor.{Actor, Props, ActorSystem, ActorRef, PoisonPill}
import collection.mutable.Map
import kaning.messages._
import kaning.actors._
import com.typesafe.config.ConfigFactory

object ChatServerApplication extends App {
  println("Starting Akka Chat Server Actor")
  val system = ActorSystem("AkkaChat", ConfigFactory.load.getConfig("chatserver"))
  val server = system.actorOf(Props[ChatServerActor], name = "chatserver")
  server ! StartUp
}

class ChatServerActor extends Actor {

  val connectedClients:Map[String, ActorRef] = Map() //<-- this is a MUTABLE map

  def receive = {

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
          connectedClients += (identity -> client)
          sender ! ChatInfo("REGISTERED SUCCESSFULLY")
          connectedClients.values.filter(_ != sender).foreach(_ ! ChatInfo(s"${identity} join this channel"))
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
        connectedClients.remove(identity).foreach(_ ! PoisonPill) //<-- this is why we use the MUTABLE map
        connectedClients.values.filter(_ != sender).foreach(_ ! ChatInfo(s"${identity} left this channel"))

    case _ => sender ! ChatInfo("Stop mumbling and articulate, you're off protocol buddy")
  }
}