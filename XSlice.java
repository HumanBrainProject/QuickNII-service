import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Arrays;

public final class XSlice extends SliceBase {
    final byte slice[];

    public XSlice(short xdim, short ydim, short zdim, View[] views, byte[][] blobs) throws Exception {
        super(xdim, ydim, zdim, views, blobs);
        slice = new byte[ydim * zdim * 4];
    }

    @Override
    protected void loop(DataInputStream dis, DataOutputStream dos) throws Exception {
        while (true) {
            byte idx = dis.readByte();
            float low = dis.readFloat();
            float scale = 255 / (dis.readFloat() - low);
            int x = dis.readInt();
            if (dis.available() == 0) {
                if (x < 0 || x >= xdim)
                    Arrays.fill(slice, (byte) 0);
                else {
                    final View v = views[idx];
                    final int q = flipbase - x;
                    final byte b0[] = blobs[v.idx];
                    final byte b1[] = blobs.length > v.idx + 1 ? blobs[v.idx + 1] : null;
                    final byte b2[] = blobs.length > v.idx + 2 ? blobs[v.idx + 2] : null;
                    final byte r[] = v.r;
                    final byte g[] = v.g;
                    final byte b[] = v.b;

                    for (int i = 0; i < ydim * zdim; i++) {
                        final int w = i * 4;
                        final int ww = q - i * xdim;
                        switch (v.type) {
                            case 1:
                                slice[w + 1] = slice[w + 2] = slice[w + 3] = (byte) Math.min(255,
                                        Math.max((((b0[ww] << 8) & 65535) - low) * scale, 0));
                                break;
                            case 2:
                                slice[w + 1] = slice[w + 2] = slice[w + 3] = (byte) Math.min(255,
                                        Math.max(((((b0[ww] << 8) + (b1[ww] & 255)) & 65535) - low) * scale, 0));
                                break;
                            case 3: {
                                final int vox = b0[ww] & 0xFF;
                                slice[w + 1] = r[vox];
                                slice[w + 2] = g[vox];
                                slice[w + 3] = b[vox];
                                break;
                            }
                            case 4: {
                                final int vox = ((b0[ww] & 0xFF) << 8) + (b1[ww] & 0xFF);
                                slice[w + 1] = r[vox];
                                slice[w + 2] = g[vox];
                                slice[w + 3] = b[vox];
                                break;
                            }
                            case 5: {
                                slice[w + 1] = b0[ww];
                                slice[w + 2] = b1[ww];
                                slice[w + 3] = b2[ww];
                                break;
                            }
                            default:
                        }
                    }

                }
                dos.write(slice);
                dos.flush();
            }
        }
    }
}
