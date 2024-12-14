package oteldemo.broker;

import jakarta.jms.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Hashtable;
import java.util.Optional;

public class BrokerConnector {
    private static final Logger logger = LogManager.getLogger(BrokerConnector.class);

    private static String QUEUE_REF = "queue/ad-request";

    public void requestAd() {
        try {
            Connection connection = null;
            InitialContext initialContext = null;
            try {

                String message_queue_host = Optional.ofNullable(System.getenv("MESSAGE_QUEUE_HOST")).orElse("messagequeue");
                String message_queue_port = Optional.ofNullable(System.getenv("MESSAGE_QUEUE_PORT")).orElse("61616");
                String message_queue_user = Optional.ofNullable(System.getenv("MESSAGE_QUEUE_PORT")).orElse("artemis");
                String message_queue_password = Optional.ofNullable(System.getenv("MESSAGE_QUEUE_PORT")).orElse("artemis");
                Hashtable env = new Hashtable();
                env.put("connectionFactory.ConnectionFactory", "tcp://" + message_queue_host + ":" + message_queue_port);

                // Step 1. Create an initial context to perform the JNDI lookup.
                initialContext = new InitialContext(env);

                // Step 2. Perform a lookup on the queue
                Queue queue = (Queue) initialContext.lookup(QUEUE_REF);

                // Step 3. Perform a lookup on the Connection Factory
                ConnectionFactory cf = (ConnectionFactory) initialContext.lookup("ConnectionFactory");

                // Step 4.Create a JMS Connection
                connection = cf.createConnection(message_queue_user, message_queue_password);

                // Step 5. Create a JMS Session
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

                // Step 6. Create a JMS Message Producer
                MessageProducer producer = session.createProducer(queue);

                // Step 7. Create a Text Message
                TextMessage message = session.createTextMessage("This is a text message");

                System.out.println("Sent message: " + message.getText());

                // Step 8. Send the Message
                producer.send(message);

                // Step 9. Create a JMS Message Consumer
                MessageConsumer messageConsumer = session.createConsumer(queue);

                // Step 10. Start the Connection
                connection.start();

                // Step 11. Receive the message
                TextMessage messageReceived = (TextMessage) messageConsumer.receive(5000);

                System.out.println("Received message: " + messageReceived.getText());
            } finally {
                // Step 12. Be sure to close our JMS resources!
                if (initialContext != null) {
                    initialContext.close();
                }
                if (connection != null) {
                    connection.close();
                }
            }
        } catch (JMSException | NamingException exception) {
            logger.error("Error while requesting ad via JMS", exception);
        }
    }

}
