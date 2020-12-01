package process;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleBiFunction;
/*

    <timestamp,sender,message>
    messages                response
    0 - hey                 4 list or no answer
    1 - coordinator alive   3 ok - add time to last time and compare
    2 - election            3 ok or 2 election
    resonses codes
    3 - ok, i received      no response
    4 - list of hosts is coming
    5 - new peer            3 ok and add peer to list

 */
public class Peer {
    final int COORDINATOR_DEFAULT =8090;
    final int MAX_LIFE = 5000 ; /// in milliseconds
    final int NO_RESPONSE_SPAN = 3000 ; /// in milliseconds, received any message in 3 seconds, alive or election
    private int port = 8090;
    private String host = "127.0.0.1"; /// default, all on local host
    private int defaultTimeOut = 1000;
    private ServerSocket serverSocket = null;
    List<Integer> peers = new ArrayList<>();
    private boolean active = true ;
    private boolean AMA_COORDINATOR = false ;
    long AliveTimeStamp = 0;

    boolean sendAndGetRespone(int port , String message , int timeOut){
        try{
            Socket s=new Socket(this.getHost(),port);
            System.out.println("sending to "+ port);
            s.setSoTimeout(timeOut);
            DataOutputStream dout=new DataOutputStream(s.getOutputStream());
            DataInputStream din = new DataInputStream(s.getInputStream());
            dout.writeUTF(message);
            String response = din.readUTF(); /// we need to decode response
            System.out.println("we got response "+ response);
            decodeResponse(response);
            dout.flush();
            dout.close();
            s.close();
            return true;
        }catch(Exception e){System.out.println(e);}
        return false;

    }
    void receivedListOfPeers(String response){
        List<Integer> l  =  decodePeers(response.substring(2)); /// remove first char
        this.setPort(l.get(l.size() -1)); //setting my port as last in list
        l.remove(l.size()- 1);///remove myself
        this.setPeers(l);
        return ;

    }
    void checkTimeStampAndSender(long timestamp , int sender){
        /// if sender is coordinator port 8090
        // we update the Alive Timestamp

        if(sender == COORDINATOR_DEFAULT){
            setAliveTimeStamp(timestamp);
        }

    }
    void decodeResponse(String response ){
        String msg=  getMessageFromResponse(response);
        int sender = getSenderPortFromResponse(response);
        long timestamp = getTimeStampFromRepsonse(response);
        char c = msg.charAt(0);
        checkTimeStampAndSender(timestamp , sender);
        ///sent msg and got this as response
        ///receive c as response
        switch (c){
            case '4':
                System.out.println("received list of peers");
                receivedListOfPeers(msg);
                break;
            case '3':
                /// received ok
                System.out.println("received okay");
                break;
            default:
                System.out.println("we cannot resolve this response");
                break;
        }
    }
    String encodeResponse(String response){
        String msg=  getMessageFromResponse(response);
        int sender = getSenderPortFromResponse(response);
        long timestamp = getTimeStampFromRepsonse(response);
        checkTimeStampAndSender(timestamp , sender);
        /// received this msg and encode a proper response and handle actions
        char c = msg.charAt(0);
        String okay_msg= "3 okay";
        switch (c){
            case '0': /// if we receive 1
                /// if we received new peer we send list of other peers
                /// adding coordinator port and other ports including last which is the port the receiver will be listening to
                this.addNewPeer();
                this.notifyWithNewPeer();
                String list_msg= new String("4 " + this.getPort()+" "+ encodePeers());
                return encodeMessage(getNowTimeStamp() , list_msg);

            ///case '5' we received new peer respond with okay
            case '5':
                notifiedWithNewPeer(msg);
                return encodeMessage(getNowTimeStamp() , okay_msg);
//                return new String("3 okay");
            default:
                return encodeMessage(getNowTimeStamp() , okay_msg);
        }
    }
    void sendAlive(){
        long lastSent = 0 ;
//        while(active){
//            if(getNowTimeStamp() - lastSent >= 2000){
            for (int peer:peers) {
                lastSent = getNowTimeStamp();
                sendAndGetRespone(peer , encodeMessage(lastSent , "1 alive"),1000);
            }
//            }
//        }
    }
    String encodePeers(){
        String ret = "";
        for (int port: peers) {
            ret += port + " ";
        }
        return ret;
    }
    List<Integer> decodePeers(String s){
        List<Integer> ret = new ArrayList<>();
        for(String num : s.split(" ")){
            ret.add(Integer.parseInt(num));
        }
        return ret;
    }
    void addNewPeer(){
        /// this called in coordinator only
        if(this.peers.size() ==0 ){
            int last = 8090;
            this.peers.add(last + 1) ;
        }
        else {
            int sz = this.peers.size();
            int last = this.peers.get(sz -1);
            this.peers.add(last+ 1);
        }
    }
    void notifyWithNewPeer(){
        /// used in coordinator
        /// don't notify last one he already got response
        /// last in list is the new peer
        for (int i = 0; i < peers.size()-1 ; i++) {
            sendAndGetRespone(peers.get(i) ,encodeMessage(getNowTimeStamp() ,"5 " +peers.get(peers.size() -1 )) ,1000);
        }

    }
    void notifiedWithNewPeer(String response){
        response = response.substring(2);
        int newPeer =Integer.parseInt( response) ;
        this.peers.add(newPeer);
        printPeers();
        return ;
    }
    void notifyElection(){
        // TODO: 12/1/2020
        /// we are supposed to be having the list in ascending order so we find our index
        /// then we notify all peers with higher priority (which is here with lower port number)//

    }
    void notifiedWithElection(String response){
        // TODO: 12/1/2020
        /// we get notified with an election message,
        // if the port is lower than us we send ok
        // otherwise we send election message

    }
    boolean bindServerSocket(){
        try {
            serverSocket = new ServerSocket(this.getPort());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
    boolean receiveAndGiveResponse(int timeOut){
        try{
//            ServerSocket ss=new ServerSocket(this.getPort());
            if(timeOut> 0)
                serverSocket.setSoTimeout(timeOut);
            Socket s=serverSocket.accept();//establishes connection
            DataInputStream din=new DataInputStream(s.getInputStream());
            DataOutputStream dout=new DataOutputStream(s.getOutputStream());
            String str=din.readUTF();
            System.out.println("message :  "+str);
            // TODO: 11/30/2020  handle the cases when str is null
            dout.writeUTF(encodeResponse(str));
            dout.flush();
            dout.close();
            din.close();
            serverSocket.close();
            return true;
        }catch(Exception e){
            /// if we timed out, we send election
            System.out.println(e);

        }
        return false;
    }

    void BullyAlgorithm(){
        if(sendHeyToCoordinator()){
            /// if we can ping coordinator now we know our port and listening to it
            /// and know other processes
            Listen();/// we listen to our port
            // TODO: 11/30/2020 decide when to consider coordinator down
        }else {
            System.out.println("cannot connect to coordinator");
            /// i'm coordinator rn and ill add myself to list
            AMA_COORDINATOR = true;
            Listen();
        }
    }
    void Listen(){
        System.out.println("I'm listening to " + this.getPort());
        while(active){
            if(AMA_COORDINATOR){
                /// when timed out the socket is not closed so we don't need to open it again
                if(serverSocket == null||serverSocket.isClosed())
                    bindServerSocket();
                receiveAndGiveResponse(2000); ///listen for 2 second and send alive wait indefinitely
                sendAlive();
            }else {
                /// when timed out the socket is not closed so we don't need to open it again
                if(serverSocket == null||serverSocket.isClosed())
                    bindServerSocket();

                receiveAndGiveResponse(NO_RESPONSE_SPAN); /// wait 3 seconds
            }

        }
    }
    boolean sendHeyToCoordinator(){
        /// we create a peer here because the default settings is it of coordinator
        String msg = encodeMessage(getNowTimeStamp() ,"0 hey");
        return sendAndGetRespone(8090,msg,4000); /// wait for 4 seconds
    }
    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    public List<Integer> getPeers() {
        return peers;
    }

    public void setPeers(List<Integer> peers) {
        this.peers = peers;
    }
    void printPeers(){
        // TODO: 12/1/2020 remove
        for (int port: peers) {
            System.out.print(port + " ");
        }
        System.out.println("");

    }
    long getNowTimeStamp(){
        return new Timestamp(System.currentTimeMillis()).getTime();
    }
    String encodeMessage(long timeStamp,String message){
        return timeStamp+","+ this.getPort() + ","+message;
    }
    long getTimeStampFromRepsonse(String response){
        String temp[] = response.split(",");
        long ret = Long.parseLong(temp[0]);
        return ret;
    }
    int getSenderPortFromResponse(String response){
        String temp[] = response.split(",");
        return Integer.parseInt(temp[1]);
    }
    String getMessageFromResponse(String response){
        String temp[] = response.split(",");
        return temp[2];
    }
    Timestamp getTimeStamp(long timeInMillis){
        return new Timestamp(timeInMillis);
    }
    public long getAliveTimeStamp() {
        return AliveTimeStamp;
    }

    public void setAliveTimeStamp(long aliveTimeStamp) {
        AliveTimeStamp = aliveTimeStamp;
    }
}
