import com.rabbitmq.client.*;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Chatter {

    private static final String EXCHANGE_NAME = "generalTopic";
    private static final String EXCHANGE_NAME2 = "directTopic";
    private static String nickname;
    private static ArrayList<String> joinMsgs;

    public static void main(String[] argv) throws Exception {
        addJoinMessages();
        ConnectionFactory factory = new ConnectionFactory();
        //factory.setHost("donkey.rmq.cloudamqp.com");
        //factory.setPort(1883);
        factory.setUri("amqp://aqqhwsok:QgZCD05h7EBfYrmKBsHmoOTcE4FDXpcP@donkey.rmq.cloudamqp.com/aqqhwsok");
        Connection conn = factory.newConnection();
        Channel channel = conn.createChannel();

        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);
        channel.exchangeDeclare(EXCHANGE_NAME2, BuiltinExchangeType.DIRECT);

        String queueName = channel.queueDeclare().getQueue(); //Random Queue Name
        channel.queueBind(queueName, EXCHANGE_NAME, "");

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) throws IOException {
                String msg = new String(body, "UTF-8");

                System.out.println(msg);
            }
        };
        channel.basicConsume(queueName, true, consumer);

        Scanner s = new Scanner(System.in);
        System.out.println("Digite o seu nome:");
        nickname = s.nextLine();

        channel.queueBind(queueName, EXCHANGE_NAME2, nickname);

        String joinMsg = String.format(generateRandomJoinMessage(), nickname);
        channel.basicPublish(EXCHANGE_NAME, "", null, joinMsg.getBytes("UTF-8"));

        boolean connected = true;

        while(connected){
            String msg = s.nextLine();

            if(msg.startsWith("/")){ //Command
                int finish;
                if(msg.indexOf(' ') != -1){
                    finish = msg.indexOf(' ');
                } else {
                    finish = msg.length();
                }
                switch(msg.substring(1, finish)){
                    case "w":
                        String[] params = msg.split("\\s+", 3);
                        if(params.length < 3){
                            System.out.println("USO: /w Nome Mensagem");
                            System.out.println("Exemplo: /w Victor Ola, esta e uma mensagem privada!");
                        } else {
                            Date date = new Date();
                            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                            String sDate = sdf.format(date);

                            String whisper = "[" + sDate + "] " + "Mensagem privada de " + nickname + ": " + params[2];
                            channel.basicPublish(EXCHANGE_NAME2, params[1], null, whisper.getBytes());
                            whisper = "[" + sDate + "] " + "Mensagem privada para " + params[1] + ": " + params[2];
                            channel.basicPublish(EXCHANGE_NAME2, nickname, null, whisper.getBytes());
                        }
                        break;
                    case "sair":
                        String leaveMsg = nickname + " saiu da sala!";
                        System.out.println(leaveMsg);
                        channel.basicPublish(EXCHANGE_NAME, "", null, leaveMsg.getBytes("UTF-8"));
                        connected = false;
                        break;
                    case "ajuda":
                        System.out.println("------------[Comandos]------------");
                        System.out.println("/w - Envia uma mensagem privada");
                        System.out.println("/sair - desconecta do chat");
                    default:
                        System.out.println("Este comando (/"+ msg.substring(1, finish) +") nao existe!");
                        break;
                }
            } else {
                Date date = new Date();
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                String sDate = sdf.format(date);

                String finalMsg = "[" + sDate + "] " + nickname + ": " + msg;

                channel.basicPublish(EXCHANGE_NAME, "", null, finalMsg.getBytes("UTF-8"));
            }
        }

        channel.close();
        conn.close();
    }

    private static String generateRandomJoinMessage() {
        return joinMsgs.get((new Random()).nextInt(joinMsgs.size()));
    }

    private static void addJoinMessages() {
        joinMsgs = new ArrayList<String>();

        joinMsgs.add("Saiam daqui! %s acabou de chegar.");
        joinMsgs.add("Pessoal, %s entrou. Finjam que estao ocupados.");
        joinMsgs.add("Eu desisto. Ate o %s agora?");
        joinMsgs.add("%s chegou atrasado, pra variar.");
        joinMsgs.add("Shhhh! %s acabou de entrar.");
    }

}