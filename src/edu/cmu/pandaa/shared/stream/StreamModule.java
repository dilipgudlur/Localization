

interface StreamModule {
    void initialize(StreamHeader inHeader);
    StreamHeader getOutHeader();
    StreamFrame process(StreamFrame inFrame);
    void close();
}

public void go(FrameStream in, FrameStream out, StreamModule m1, StreamModule m2) {

    StreamHeader headerIn = in.getHeader();
    m1.initialize(headerIn);
    m2.initialize(m1.getOutHeader());
    out.setHeader(m2.getOutHeader());

    while ((StreamFrame frame = in.recvFrame()) != null) {
	frame = m1.process(frame);
	frame = m2.process(frame);
	out.sendFrame(frame);
    }

    m1.close();
    m2.close();
}
