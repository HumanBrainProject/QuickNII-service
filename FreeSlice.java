import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Arrays;

public final class FreeSlice extends SliceBase {
    public FreeSlice(final short xdim, final short ydim, final short zdim, final View views[], final byte blobs[][])
            throws Exception {
        super(xdim, ydim, zdim, views, blobs);
        slice = new byte[0];
        auxslice = new byte[0];
    }

    private byte slice[];
    private byte auxslice[];

    @Override
    protected void loop(DataInputStream dis, DataOutputStream dos) throws Exception {
        while (true) {
            final byte idx = dis.readByte();
            final float low = dis.readFloat();
            final float scale = 255 / (dis.readFloat() - low);
            final double ox = dis.readDouble();
            final double oy = dis.readDouble();
            final double oz = dis.readDouble();
            final double ux = dis.readDouble();
            final double uy = dis.readDouble();
            final double uz = dis.readDouble();
            final double vx = dis.readDouble();
            final double vy = dis.readDouble();
            final double vz = dis.readDouble();
            if (dis.available() == 0) {
                final int width = (int) Math.sqrt(ux * ux + uy * uy + uz * uz) + 1;
                final int height = (int) Math.sqrt(vx * vx + vy * vy + vz * vz) + 1;

                final int l = width * height * 4;
                if (slice.length != l)
                    slice = new byte[l];
                else
                    Arrays.fill(slice, (byte) 0);

                final View v = views[idx & 127];
                if (v.type == 3 || v.type == 4) {
                    if (auxslice.length != width * height * (v.type - 2))
                        auxslice = new byte[width * height * (v.type - 2)];
                    else
                        Arrays.fill(auxslice, (byte) 0);
                }

                final int w4 = width * 4;
                final int a = xdim * ydim;

                final byte b0[] = blobs[v.idx];
                final byte b1[] = blobs.length > v.idx + 1 ? blobs[v.idx + 1] : null;
                final byte b2[] = blobs.length > v.idx + 2 ? blobs[v.idx + 2] : null;

                final byte r[] = v.r;
                final byte g[] = v.g;
                final byte b[] = v.b;

                for (int y = 0; y < height; y++) {
                    final double hx = ox + vx * y / (height);
                    final double hy = oy + vy * y / (height);
                    final double hz = oz + vz * y / (height);
                    final int base = w4 * y;
                    for (int x = 0; x < width; x++) {
                        final int lx = (int) (hx + ux * x / (width));
                        final int ly = (int) (hy + uy * x / (width));
                        final int lz = (int) (hz + uz * x / (width));
                        if (lx >= 0 && lx < xdim && ly >= 0 && ly < ydim && lz >= 0 && lz < zdim) {
                            final int w = base + x * 4;
                            final int ww = lx + ly * xdim + lz * a;
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
                                    final int vox = (auxslice[w >> 2] = b0[ww]) & 0xFF;
                                    if (idx > 0) {
                                        slice[w + 1] = r[vox];
                                        slice[w + 2] = g[vox];
                                        slice[w + 3] = b[vox];
                                    } else {
                                        if (x == 0)
                                            slice[w] = vox != 0 ? 0 : (byte) 255;
                                        else if (y == 0)
                                            slice[w] = vox != 0 ? 0 : (byte) 255;
                                        else
                                            slice[w] = (vox == auxslice[(w >> 2) - 1])
                                                    && (vox == auxslice[(w >> 2) - width]) ? (byte) 255 : 0;
                                        if ((slice[w] == (byte) 255) && ((x == width - 1) || (y == height - 1)))
                                            slice[w] = vox != 0 ? 0 : (byte) 255;
                                        slice[w + 2] = slice[w] = (byte) (slice[w] ^ 255);
                                    }
                                    break;
                                }
                                case 4:
                                    final byte hi = auxslice[w >> 1] = b0[ww];
                                    final byte lo = auxslice[(w >> 1) + 1] = b1[ww];
                                    final int vox = ((hi & 0xFF) << 8) + (lo & 0xFF);
                                    if (idx > 0) {
                                        slice[w + 1] = r[vox];
                                        slice[w + 2] = g[vox];
                                        slice[w + 3] = b[vox];
                                    } else {
                                        if (x == 0)
                                            slice[w] = vox != 0 ? 0 : (byte) 255;
                                        else if (y == 0)
                                            slice[w] = vox != 0 ? 0 : (byte) 255;
                                        else
                                            slice[w] = (hi == auxslice[(w >> 1) - 2]) && (lo == auxslice[(w >> 1) - 1])
                                                    && (hi == auxslice[(w >> 1) - width * 2])
                                                    && (lo == auxslice[(w >> 1) - width * 2 + 1]) ? (byte) 255 : 0;
                                        if ((slice[w] == (byte) 255) && ((x == width - 1) || (y == height - 1)))
                                            slice[w] = vox != 0 ? 0 : (byte) 255;
                                        slice[w + 2] = slice[w] = (byte) (slice[w] ^ 255);
                                    }
                                    break;
                                case 5: {
                                    slice[w + 1] = b0[ww];
                                    slice[w + 2] = b1[ww];
                                    slice[w + 3] = b2[ww];
                                }
                                default:
                            }
                        }
                    }
                }

                dos.writeByte(v.type);
                dos.writeInt(width);
                dos.writeInt(height);
                dos.write(slice);
                if (v.type == 3 || v.type == 4)
                    dos.write(auxslice);
                dos.flush();
            }
        }
    }
}
