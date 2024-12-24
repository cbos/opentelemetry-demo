package oteldemo.analytics;

import com.google.protobuf.ProtocolStringList;
import jakarta.jms.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import oteldemo.Demo;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Hashtable;
import java.util.List;
import java.util.Optional;

public class AnalyticsConnector {
    private static final Logger logger = LogManager.getLogger(AnalyticsConnector.class);
    public static final String ADD_COUNT = "add.count";
    public static final String ADD_REQUEST_TYPE = "add.requestType";
    public static final String ADD_RESPONSE_TYPE = "add.responseType";
    public static final String ADD_CATEGORIES = "add.categories";

    private static final String QUEUE_REF = "queue/analytics";

    Connection connection = null;
    InitialContext initialContext = null;

    private static AnalyticsConnector instance;

    private AnalyticsConnector(){
        //Private constructor for the singleton
    }

    /**
     * Singleton pattern to get the instance of AnalyticsConnector.
     * @return The singleton instance of AnalyticsConnector.
     */
    public static AnalyticsConnector getInstance() {
        if (instance == null) {
            instance = new AnalyticsConnector();
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


    public void requestAd(ProtocolStringList contextKeysList, List<Demo.Ad> allAds, String requestType, String responseType) {
        if(this.connection == null){
            startConnection();
        }
        try {
            // Perform a lookup on the queue
            Queue queue = (Queue) initialContext.lookup(QUEUE_REF);

            // Create a JMS Session
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // Create a JMS Message Producer on the queue
            MessageProducer producer = session.createProducer(queue);

            // Create the Message
            MapMessage message = session.createMapMessage();
            message.setInt(ADD_COUNT, allAds.size());
            message.setString(ADD_REQUEST_TYPE, requestType);
            message.setString(ADD_RESPONSE_TYPE, responseType);
            message.setString(ADD_CATEGORIES, contextKeysList.toString());

            // Send the Message
            producer.send(message);

        } catch (JMSException | NamingException exception) {
            logger.error("Error while requesting ad via JMS", exception);
        }
    }

}
