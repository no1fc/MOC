package me.user.moc.config;

import org.bukkit.Location;

public class ConfigManager {
    // 싱글톤 패턴 (어디서든 설정값을 불러오기 위해)
    private static ConfigManager instance;
    public static ConfigManager getInstance() {
        if (instance == null) instance = new ConfigManager();
        return instance;
    }

    // 설정 변수들 (기본값 설정)
    public boolean spawn_tf = true;      // 스폰 이동 여부
    public Location spawn_point = null;  // 스폰 좌표
    public int peace_time = 3;          // 무적 시간
    public boolean team_attack = false;  // 팀킬 가능 여부
    public boolean teammod = false;       // 팀전 활성화 여부
    public int re_point = 1;             // 리롤 횟수 (기본값은 2로 설정, 명령어로 변경 가능)
    public int start_time = 30;          // 능력 추첨 시간 (기획안의 30초)
    public boolean final_fight = true;   // 자기장 여부
    public int final_time = 300;         // 자기장 시작 시간 (5분 = 300초)
    public boolean map_end = true;       // 장벽 활성화 여부
    public double map_size = 60;       // 장벽 크기
    public int win_value = 40;           // 목표 점수
    public boolean hidden = false;       // 히든 캐릭터 여부
}