package com.ductaper.core.channel

import java.util.Date

import com.ductaper.core.events.Event.SystemEvent
import com.ductaper.core.message.Key.Timestamp
import com.ductaper._
import com.ductaper.core.CloseCapable
import com.ductaper.core.exchange.{Exchange, ExchangeType}
import com.ductaper.core.message.Message
import com.ductaper.core.route._
import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.{AMQP, Channel, DefaultConsumer, Envelope}

import scala.collection.JavaConverters

/**
 * @author Zahari Dichev <zaharidichev@gmail.com>.
 */

class ChannelManager(chan: Channel, eventListener: SystemEvent ⇒ Unit) extends ChannelWrapper {

  override def declareExchange(exchangeName: String, exchangeType: ExchangeType, durable: Boolean, autoDelete: Boolean, args: Map[String, AnyRef]): Exchange = {
    chan.exchangeDeclare(exchangeName, exchangeType.name, durable, autoDelete, JavaConverters.mapAsJavaMap(args))
    Exchange(exchangeName)
  }

  override def declareExchangePassive(exchangeName: String): Exchange = {
    chan.exchangeDeclarePassive(exchangeName);
    Exchange(exchangeName)
  }

  override def declareQueue(queueDeclare: Queue): QueueDeclared = QueueDeclared(declareQueue(queueDeclare, chan))

  private def declareQueue(queueDeclare: Queue, channel: Channel): String = {
    queueDeclare match {
      case QueueDeclare(name, durable, exclusive, autodelete, args) ⇒ chan.queueDeclare(name.getOrElse(""), durable, exclusive, autodelete, JavaConverters.mapAsJavaMap(args)).getQueue
      case QueuePassive(name)                                       ⇒ chan.queueDeclarePassive(name).getQueue
      case QueueDeclared(name)                                      ⇒ chan.queueDeclarePassive(name).getQueue
    }
  }

  override def addAutoAckConsumer(queue: Queue, consumer: Message ⇒ Unit): CloseCapable = {
    val nameOfQueue = declareQueue(queue).name
    val nativeConsumer = new DefaultConsumer(chan) {
      override def handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: Array[Byte]): Unit = {
        val mess: Message = Message(properties, body);
        consumer(mess)
      }
    }
    new ConsumerHandle(chan, chan.basicConsume(nameOfQueue, true, nativeConsumer))
  }

  override def queueBind(queue: QueueDeclared, exchange: Exchange, routingKey: RoutingKey): Unit = chan.queueBind(queue.name, exchange.name, routingKey.name)

  override def close(): Unit = chan.close()

  override def send(route: BrokerRoutingData, message: Message): Unit = {
    val timestampProperty = Timestamp → new Date()
    val basicProps: BasicProperties = (message.messageProperties + timestampProperty).toJavaBasicProps
    sendViaChannel(route.exchange.name, route.routingKey.name, basicProps, message.body.toArray)
  }

  private def sendViaChannel(exchangeName: String, routingKey: String, props: BasicProperties, body: Array[Byte]) = {
    chan.basicPublish(exchangeName, routingKey, props, body)

  }
}