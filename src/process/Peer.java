package process;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleBiFunction;

/*
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
    private int port = 8090;
    private String host = "127.0.0.1"; /// default, all on local host
    private int defaultTimeOut = 1000;
//    private ServerSocket serverSocket = null;
    List<Integer> peers = new ArrayList<>();
    boolean active = true ;

    boolean sendAndGetRespone(int port , String message , int timeOut){
        try{
            Socket s=new Socket(this.getHost(),port);
            System.out.println("sending to "+ port);
            s.setSoTimeout(timeOut);
            DataOutputStream dout=new DataOutputStream(s.getOutputStream());
            DataInputStream din = new DataInputStream(s.getInputStream());
            dout.writeUTF(message);
            String response = din.readUTF();
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
    void decodeResponse(String response ){
        char c = response.charAt(0);
        ///sent msg and got this as response
        ///receive c as response
        switch (c){
            case '4':
                System.out.println("received list of peers");
                receivedListOfPeers(response);
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
        /// received this msg and encode a proper response and handle actions
        char c = response.charAt(0);
        switch (c){
            case '0': /// if we receive 1
                /// if we received new peer we send list of other peers
                /// adding coordinator port and other ports including last which is the port the receiver will be listening to
                this.addNewPeer();
                this.notifyWithNewPeer();
                return new String("4 " + this.getPort()+" "+ encodePeers());

            ///case '5' we received new peer respond with okay
            case '5':
                notifiedWithNewPeer(response);
                return new String("3 okay");
            default:
                return new String("3 okay");
        }
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
            sendAndGetRespone(peers.get(i) ,"5 "+ peers.get(peers.size() -1 ),1000);
        }

    }
    void notifiedWithNewPeer(String response){
        response = response.substring(2);
        int newPeer =Integer.parseInt( response) ;
        this.peers.add(newPeer);
        printPeers();
        return ;
    }
    boolean receiveAndGiveResponse(int timeOut){
        try{
            ServerSocket ss=new ServerSocket(this.getPort());
            if(timeOut> 0)
                ss.setSoTimeout(timeOut);
            Socket s=ss.accept();//establishes connection
            DataInputStream din=new DataInputStream(s.getInputStream());
            DataOutputStream dout=new DataOutputStream(s.getOutputStream());
            String str=din.readUTF();
            System.out.println("message :  "+str);
            // TODO: 11/30/2020  handle the cases when str is null
            dout.writeUTF(encodeResponse(str));
            dout.flush();
            dout.close();
            din.close();
            ss.close();
            return true;
        }catch(Exception e){System.out.println(e);}
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
            Listen();
        }
    }
    void Listen(){
        System.out.println("I'm listening to " + this.getPort());
        while(active){
           receiveAndGiveResponse(0); /// wait indefinitely

        }
    }
    boolean sendHeyToCoordinator(){
        /// we create a peer here because the default settings is it of coordinator
        return sendAndGetRespone(8090,"0 hey",1000);
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
}
