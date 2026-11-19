package com.healthcare.api.push;

import com.healthcare.api.state.AlarmEntity;
import com.healthcare.api.state.AlarmRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 每分钟检查一次闹钟，到点后通过 Web Push 提醒。
 * 单实例部署下用内存集合去重；多实例部署需要分布式锁。
 */
@Component
public class AlarmPushScheduler {

    private static final Logger log = LoggerFactory.getLogger(AlarmPushScheduler.class);
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final AlarmRepository alarms;
    private final WebPushService pushService;
    private final Set<String> fired = ConcurrentHashMap.newKeySet();

    public AlarmPushScheduler(AlarmRepository alarms, WebPushService pushService) {
        this.alarms = alarms;
        this.pushService = pushService;
    }

    @Scheduled(cron = "0 * * * * *")
    public void tick() {
        ZonedDateTime now = ZonedDateTime.now(ZONE);
        String date = now.toLocalDate().toString();
        for (AlarmEntity alarm : alarms.findByEnabledTrue()) {
            if (!AlarmMatcher.matches(alarm.getTime(), alarm.getRepeatDays(), now)) {
                continue;
            }
            String key = alarm.getId() + ":" + date + ":" + alarm.getTime();
            if (!fired.add(key)) {
                continue;
            }
            int sent = pushService.sendToUser(
                    alarm.getUserId(),
                    "⏰ 闹钟提醒",
                    alarm.getLabel() + " · " + alarm.getTime(),
                    "/#/sleep",
                    "alarm-" + alarm.getId());
            log.info("Alarm {} triggered, pushed to {} device(s)", alarm.getId(), sent);
            if (alarm.getRepeatDays() == null || alarm.getRepeatDays().isBlank()) {
                alarm.setEnabled(false);
                alarms.save(alarm);
            }
        }
        // 防止长期运行时集合无限增长
        if (fired.size() > 5000) {
            fired.clear();
        }
    }
}
