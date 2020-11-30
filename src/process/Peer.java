package process;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.CharBuffer;
import java.util.List;
/*
    messages
    0 - hey
    1 - coordinator alive
    2 - election
    3 - ok, i received
    4 - list of hosts is coming


 */
public class Peer {
    private int port = 8090;
    private String host = "127.0.0.1"; /// default, all on local host
    private int defaultTimeOut = 1000;
//    private ServerSocket serverSocket = null;
    List<Integer> peers = null;
    boolean active = true ;
    Socket makeConnection(Peer peer){
        try {
            return new Socket(peer.getHost(), peer.getPort());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    boolean send(Peer peer , String message){
        return send(peer , message , defaultTimeOut);
    }
    boolean send(Peer peer, String message, int timeOut )  {
        try{
            Socket s=new Socket(peer.getHost(),peer.getPort());
            s.setSoTimeout(timeOut);
            DataOutputStream dout=new DataOutputStream(s.getOutputStream());
            dout.writeUTF(message);
            dout.flush();
            dout.close();
            s.close();
            return true;
        }catch(Exception e){System.out.println(e);}
        return false;
    }
    String receive(){
        return receive(defaultTimeOut);
    }
    String encodePeers(){
        String ret = "";
        for (int port: peers) {
            ret += port + " ";
        }
        return ret;
    }
    String getRespone(char c){
        switch (c){
            case '0':
                /// if we received new peer we send list of other peers
                    return new String("4 " + encodePeers());
            default:
                System.out.println("we can't find a suitable response");
                return null;
        }
    }

    void respond(int timeOut){
        try{
            ServerSocket ss=new ServerSocket(this.getPort());
            if(timeOut> 0)
                ss.setSoTimeout(timeOut);
            Socket s=ss.accept();//establishes connection
            DataInputStream dis=new DataInputStream(s.getInputStream());
            DataOutputStream dout=new DataOutputStream(s.getOutputStream());
            String str=(String)dis.readUTF();
            System.out.println("message :  "+str);

            ss.close();
            return ;
        }catch(Exception e){System.out.println(e);}
        return ;
    }
    String receive(int timeOut) {
        try{
            ServerSocket ss=new ServerSocket(this.getPort());
            if(timeOut> 0)
                ss.setSoTimeout(timeOut);
            Socket s=ss.accept();//establishes connection
            DataInputStream dis=new DataInputStream(s.getInputStream());
            String str=(String)dis.readUTF();
            System.out.println("message= "+str);
            ss.close();
            return str;
        }catch(Exception e){System.out.println(e);}
        return null;
    }

    void BullyAlgorithm(){
        if(sendHeyToCoordinator()){
            String response = receive();
            if(response!= null){
                System.out.println(response);
            }
            else {
                // we didn't receive
                System.out.println("we didn't receive");
            }
        }else {
            System.out.println("cannot connect to coordinator");
            /// i'm coordinator rn and ill add myself to list
            Listen();
        }
    }
    void Listen(){
        System.out.println("I'm listening to " + this.getPort());
        while(active){
           String response =  receive(0); /// wait indefinitly
            switch (response.charAt(0)){
                case '0':
                        /// hey case we add the new peer to our list and send him port to listen to
                    break;
                case '1':

                    break;
                case '2':

                    break;
            }
        }
    }
    boolean sendHeyToCoordinator(){
        return send(new Peer(),"0 hey");
    }
    void print(String msg){
        System.out.println(msg);
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
}
