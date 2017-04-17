package ee.ttu.idu0080.raamatupood.client;

import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.log4j.Logger;

import ee.ttu.idu0080.raamatupood.server.EmbeddedBroker;
import ee.ttu.idu0080.raamatupood.types.Tellimus;
import ee.ttu.idu0080.raamatupood.types.TellimuseRida;

/**
 * JMS sõnumite tarbija. Ühendub broker-i urlile
 * 
 * @author Allar Tammik
 * @date 08.03.2010
 */
public class Consumer {
	private static final Logger log = Logger.getLogger(Consumer.class);
	private String SUBJECT = "tellimuse.saatmine";
	private String SUBJECT_ANSWER = "tellimuse.vastuvotmine";
	private String user = ActiveMQConnection.DEFAULT_USER;
	private String password = ActiveMQConnection.DEFAULT_PASSWORD;
	private String url = EmbeddedBroker.URL;
	private MessageProducer producer;
	private Session session;

	public static void main(String[] args) {
		Consumer tellijaTool = new Consumer();
		tellijaTool.run();
	}

	public void run() {
		Connection connection = null;
		try {
			log.info("Connecting to URL: " + url);
			log.info("Consuming queue : " + SUBJECT);

			// 1. Loome ühenduse
			ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
					user, password, url);
			connection = connectionFactory.createConnection();

			// Kui ühendus kaob, lõpetatakse Consumeri töö veateatega.
			connection.setExceptionListener(new ExceptionListenerImpl());

			// Käivitame ühenduse
			connection.start();

			// 2. Loome sessiooni
			/*
			 * createSession võtab 2 argumenti: 1. kas saame kasutada
			 * transaktsioone 2. automaatne kinnitamine
			 */
			session = connection.createSession(false,
					Session.AUTO_ACKNOWLEDGE);

			// Loome teadete sihtkoha (järjekorra). Parameetriks järjekorra nimi
			Destination destination = session.createQueue(SUBJECT);
			Destination answerDestination = session.createQueue(SUBJECT_ANSWER);

			// 3. Teadete vastuvõtja ja saatja
			MessageConsumer consumer = session.createConsumer(destination);
			producer = session.createProducer(answerDestination);

			// Kui teade vastu võetakse käivitatakse onMessage()
			consumer.setMessageListener(new MessageListenerImpl());

		} catch (Exception e) {
			e.printStackTrace();
		} 
	}

	/**
	 * Käivitatakse, kui tuleb sõnum
	 */
	class MessageListenerImpl implements javax.jms.MessageListener {

		public void onMessage(Message message) {
			if (message instanceof ObjectMessage) {
				try {
					ObjectMessage objectMessage = (ObjectMessage) message;

					Tellimus tellimus = (Tellimus) objectMessage.getObject();
					log.info("Received message: " + tellimus.toString());

					long tooteid = 0;
					BigDecimal hind = new BigDecimal(0);
					for(TellimuseRida rida : tellimus.getTellimuseRead()) {
						long tooteidReas = rida.getKogus();
						tooteid += tooteidReas;
						hind = hind.add(new BigDecimal(tooteidReas).multiply(rida.getToode().getHind()));
					}
					
					sendAnswer("Tooteid: " + tooteid + ", hind: " + hind.setScale(2, RoundingMode.CEILING));
				} catch (JMSException e) {
					log.warn("Caught: " + e);
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Käivitatakse, kui tuleb viga.
	 */
	class ExceptionListenerImpl implements javax.jms.ExceptionListener {

		public synchronized void onException(JMSException ex) {
			log.error("JMS Exception occured. Shutting down client.");
			ex.printStackTrace();
		}
	}
	
	private void sendAnswer(String message) {
		try {
			TextMessage answer = session.createTextMessage(message);
			log.debug("Sending answer: " + answer.getText());
			producer.send(answer);
		} catch (JMSException e) {
			log.warn("Caught: " + e);
			e.printStackTrace();
		}
	}
}