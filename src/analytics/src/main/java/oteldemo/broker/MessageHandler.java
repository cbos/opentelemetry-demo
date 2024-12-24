package oteldemo.broker;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.jms.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Hashtable;
import java.util.Optional;

public class MessageHandler {
    private static final Logger logger = LogManager.getLogger(MessageHandler.class);
    public static final String ADD_COUNT = "add.count";
    public static final String ADD_REQUEST_TYPE = "add.requestType";
    public static final String ADD_RESPONSE_TYPE = "add.responseType";
    public static final String ADD_CATEGORIES = "add.categories";

    private static final String QUEUE_REF = "queue/analytics";

    Connection connection = null;
    InitialContext initialContext = null;

    private static MessageHandler instance;

    private MessageHandler() {
        //Private constructor for the singleton
    }

    /**
     * Singleton pattern to get the instance of AnalyticsConnector.
     *
     * @return The singleton instance of AnalyticsConnector.
     */
    public static MessageHandler getInstance() {
        if (instance == null) {
            instance = new MessageHandler();
            instance.startConnection();
        }
        return instance;
    }

    private void startConnection() {
        try {
            String message_queue_host = Optional.ofNullable(System.getenv("MESSAGE_QUEUE_HOST")).orElse("messagequeue");
            String message_queue_port = Optional.ofNullable(System.getenv("MESSAGE_QUEUE_PORT")).orElse("61616");
            String message_queue_user = Optional.ofNullable(System.getenv("MESSAGE_QUEUE_USER")).orElse("artemis");
            String message_queue_password = Optional.ofNullable(System.getenv("MESSAGE_QUEUE_PASSWORD")).orElse("artemis");
            Hashtable<String, String> env = new Hashtable<>();
            env.put("connectionFactory.ConnectionFactory", "tcp://" + message_queue_host + ":" + message_queue_port);

            // Create an initial context to perform the JNDI lookup.
            initialContext = new InitialContext(env);

            // Perform a lookup on the Connection Factory
            ConnectionFactory cf = (ConnectionFactory) initialContext.lookup("ConnectionFactory");

            // Create a JMS Connection
            connection = cf.createConnection(message_queue_user, message_queue_password);
            connection.start();
        } catch (JMSException | NamingException exception) {
            logger.error("Error while creating the JMS connection", exception);
        }

    }

    public void close() {
        try {
            if (initialContext != null) {
                initialContext.close();
                initialContext = null;
            }
            if (connection != null) {
                connection.close();
                connection = null;
            }
        } catch (JMSException | NamingException exception) {
            logger.error("Error while closing the JMS connection", exception);
        }
    }

    private void handleMessage(Session session,  MapMessage message, MessageProducer producer) {
        try {

            if (message != null) {
                message.acknowledge();
                logger.info("Received a message");

                // Create the Message
//            message.setInt(ADD_COUNT, allAds.size());
//            message.setString(ADD_REQUEST_TYPE, requestType);
//            message.setString(ADD_RESPONSE_TYPE, responseType);
//            message.setString(ADD_CATEGORIES, contextKeysList.toString());

                if (message.getJMSReplyTo() != null) {
                    MapMessage response = session.createMapMessage();

                    response.setInt("Sqn", 1);
                    response.setLong("Time", 1L);

                    //Set the correlation ID from the received message to be the correlation id of the response message
                    //this lets the client identify which message this is a response to if it has more than
                    //one outstanding message to the server
                    response.setJMSCorrelationID(message.getJMSCorrelationID());
                    response.setJMSDestination(message.getJMSReplyTo());

                    //Send the response to the Destination specified by the JMSReplyTo field of the received message,
                    //this is presumably a temporary queue created by the client
                    producer.send(message.getJMSReplyTo(), response);
                }
            } else {
                Thread.sleep(100);
            }
        } catch (JMSException | InterruptedException exception) {
            logger.error("Error while requesting ad via JMS", exception);
        }
    }

    public Runnable handleMessageRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                if (connection == null) {
                    startConnection();
                }
                try {
                    // Perform a lookup on the queue
                    Queue queue = (Queue) initialContext.lookup(QUEUE_REF);

                    // Create a JMS Session
                    Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

                    // Create consumer and producer
                    MessageConsumer consumer = session.createConsumer(queue);
                    MessageProducer producer = session.createProducer(null);

                    while (true) {
                        MapMessage message = (MapMessage) consumer.receiveNoWait();
                        handleMessage(session, message, producer);
                    }
                } catch (JMSException | NamingException exception) {
                    logger.error("Error while requesting ad via JMS", exception);
                }
            }
        };

    }
}
