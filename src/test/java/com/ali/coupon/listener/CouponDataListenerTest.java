package com.ali.coupon.listener;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.ali.coupon.dto.BatchUserDto;
import com.ali.coupon.mq.BatchRocketMQProducer;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CouponDataListenerTest {

    @Test
    void shouldBatchProcessEvery500Rows() throws IOException {
        BatchRocketMQProducer mockProducer = mock(BatchRocketMQProducer.class);

        // 构造 1200 行数据 → 应产生 3 批 (500+500+200)
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ExcelWriter writer = EasyExcel.write(out, BatchUserDto.class).build();
        WriteSheet sheet = EasyExcel.writerSheet().build();

        List<BatchUserDto> rows = new ArrayList<>();
        for (int i = 0; i < 1200; i++) {
            BatchUserDto dto = new BatchUserDto();
            dto.setUserId((long) (1000 + i));
            dto.setTemplateId(1L);
            dto.setActivityId(10L);
            dto.setShopNumber("SHOP001");
            dto.setCouponAmount("5.00");
            rows.add(dto);
        }
        writer.write(rows, sheet);
        writer.finish();

        CouponDataListener listener = new CouponDataListener("test-job-1", mockProducer);
        EasyExcel.read(new ByteArrayInputStream(out.toByteArray()), BatchUserDto.class, listener)
                .sheet()
                .doRead();

        assertEquals(1200, listener.getTotalUsers());
        assertEquals(3, listener.getTotalBatches());
        verify(mockProducer, times(3)).send(any());
    }

    @Test
    void shouldHandleSmallFilesUnder500Rows() throws IOException {
        BatchRocketMQProducer mockProducer = mock(BatchRocketMQProducer.class);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ExcelWriter writer = EasyExcel.write(out, BatchUserDto.class).build();
        WriteSheet sheet = EasyExcel.writerSheet().build();

        List<BatchUserDto> rows = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            BatchUserDto dto = new BatchUserDto();
            dto.setUserId((long) i);
            dto.setTemplateId(1L);
            dto.setActivityId(10L);
            rows.add(dto);
        }
        writer.write(rows, sheet);
        writer.finish();

        CouponDataListener listener = new CouponDataListener("test-job-2", mockProducer);
        EasyExcel.read(new ByteArrayInputStream(out.toByteArray()), BatchUserDto.class, listener)
                .sheet()
                .doRead();

        assertEquals(100, listener.getTotalUsers());
        assertEquals(1, listener.getTotalBatches());
        verify(mockProducer, times(1)).send(any());
    }
}
