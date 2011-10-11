

interface StreamModule {
    StreamHeader initialize(StreamHeader inHeader);
    StreamFrame process(StreamFrame inFrame);
    void close();
}

public void go(FrameStream in, FrameStream out, StreamModule m1, StreamModule m2) {

    StreamHeader header = in.getHeader();
    header = m1.initialize(header);
    header = m2.initialize(header);
    out.setHeader(header);

    while ((StreamFrame frame = in.recvFrame()) != null) {
	frame = m1.process(frame);
	frame = m2.process(frame);
	out.sendFrame(frame);
    }

    m1.close();
    m2.close();
}
