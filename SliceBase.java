import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public abstract class SliceBase extends Thread {
    final View views[];
    final byte blobs[][];
    final ServerSocket ss;
    final short xdim;
    final short ydim;
    final short zdim;
    final int flipbase;

    public SliceBase(final short xdim, final short ydim, final short zdim, final View views[], final byte blobs[][])
            throws Exception {
        this.views = views;
        this.blobs = blobs;
        this.xdim = xdim;
        this.ydim = ydim;
        this.zdim = zdim;
        flipbase = xdim * ydim * zdim - 1;
        ss = new ServerSocket(0, 0, InetAddress.getLoopbackAddress());
//        ss.setSoTimeout(10000);
        start();
    }

    public int getLocalPort() {
        return ss.getLocalPort();
    }

    @Override
    public void run() {
        try {
            final Socket s = ss.accept();
            final DataInputStream dis = new DataInputStream(s.getInputStream());
            final DataOutputStream dos = new DataOutputStream(s.getOutputStream());
            loop(dis, dos);
        } catch (Exception ex) {
            synchronized (System.err) {
                ex.printStackTrace();
            }
        }
    }

    protected abstract void loop(DataInputStream dis, DataOutputStream dos) throws Exception;
}
