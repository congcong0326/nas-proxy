package org.congcong.nasproxy.common.util;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;

public class ByteBufSplitter {


    public static List<ByteBuf> splitByteBuf(ByteBuf source) {
        //一些客户端无法处理较大长度的chunk，在这里对ByteBuf进行逻辑上的分片
        //v2rayN无法处理长度超过65536的数据块，会保存unexpect eof
        return splitByteBuf(source, 50000);
    }

    public static List<ByteBuf> splitByteBuf(ByteBuf source, int maxChunkSize) {
        List<ByteBuf> chunks = new ArrayList<>();

        int readableBytes = source.readableBytes();
        int offset = 0;

        while (offset < readableBytes) {
            int bytesToRead = Math.min(readableBytes - offset, maxChunkSize);
            ByteBuf chunk = source.slice(offset, bytesToRead);
            chunks.add(chunk);
            offset += bytesToRead;
        }

        return chunks;
    }
}
