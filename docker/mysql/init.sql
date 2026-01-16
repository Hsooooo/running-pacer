-- users (단일 사용자라도 FK 일관성 위해 둠)
create table users (
  user_id bigint primary key auto_increment,
  nickname varchar(50) not null,
  created_at datetime not null default current_timestamp
) engine=InnoDB;

-- Strava 원본 payload 저장(멱등/디버깅/리플레이)
create table raw_strava_payloads (
  payload_id bigint primary key auto_increment,
  user_id bigint not null,
  provider varchar(20) not null default 'STRAVA',
  activity_id bigint null,
  payload_type varchar(30) not null, -- 'activity', 'streams', 'webhook_event'
  payload_json json not null,
  created_at datetime not null default current_timestamp,
  key idx_raw_user_created (user_id, created_at),
  key idx_raw_activity (activity_id),
  constraint fk_raw_user foreign key (user_id) references users(user_id)
) engine=InnoDB;

-- activities (메타)
create table activities (
  activity_id bigint primary key auto_increment,
  user_id bigint not null,
  source varchar(20) not null default 'STRAVA',
  source_activity_id bigint not null, -- Strava activity id
  sport_type varchar(20) not null default 'RUN',

  start_time_utc datetime not null,
  timezone varchar(50) null,

  distance_m int not null,
  moving_time_s int not null,
  elapsed_time_s int not null,

  avg_hr smallint null,
  max_hr smallint null,
  avg_cadence smallint null, -- spm로 저장 권장(스트라바 cadence는 half-steps일 수 있어 정규화 시점에 맞춤)
  elevation_gain_m int null,

  -- 계산/파생값(쿼리 편의)
  avg_pace_sec_per_km int null,

  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp,

  unique key uk_activity_source (source, source_activity_id),
  key idx_activity_user_time (user_id, start_time_utc),
  key idx_activity_user_dist (user_id, distance_m),
  constraint fk_activity_user foreign key (user_id) references users(user_id)
) engine=InnoDB;

-- laps (구간)
create table laps (
  lap_id bigint primary key auto_increment,
  activity_id bigint not null,
  lap_index int not null, -- 0..n-1

  distance_m int not null,
  moving_time_s int not null,
  avg_hr smallint null,
  avg_cadence smallint null,
  elevation_gain_m int null,
  avg_pace_sec_per_km int null,

  unique key uk_lap (activity_id, lap_index),
  key idx_lap_activity (activity_id),
  constraint fk_lap_activity foreign key (activity_id) references activities(activity_id) on delete cascade
) engine=InnoDB;

-- activity_streams (초 단위 스트림 통 저장)
-- 핵심: time/heartrate/velocity_smooth/cadence/watts/latlng 등 "배열"을 그대로 저장
create table activity_streams (
  activity_id bigint primary key,
  user_id bigint not null,

  -- 필요한 스트림만 저장해도 됨 (없을 수 있으니 nullable)
  streams_json json not null,
  stream_keys varchar(255) not null, -- 저장한 키 목록(예: "time,heartrate,velocity_smooth,cadence,watts,latlng")
  sample_count int null,

  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp,

  key idx_stream_user (user_id),
  constraint fk_stream_activity foreign key (activity_id) references activities(activity_id) on delete cascade,
  constraint fk_stream_user foreign key (user_id) references users(user_id)
) engine=InnoDB;

-- daily_stats (집계: AI가 주로 참고할 데이터)
create table daily_stats (
  user_id bigint not null,
  stat_date date not null,

  run_count int not null,
  total_distance_m int not null,
  total_moving_time_s int not null,

  avg_pace_sec_per_km int null,
  avg_hr smallint null,

  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp,

  primary key (user_id, stat_date),
  key idx_stats_user_date (user_id, stat_date),
  constraint fk_stats_user foreign key (user_id) references users(user_id)
) engine=InnoDB;

create table strava_tokens (
  user_id bigint primary key,
  athlete_id bigint null,

  access_token varchar(255) not null,
  refresh_token varchar(255) not null,
  expires_at bigint not null, -- epoch seconds

  scope varchar(255) null,

  created_at datetime not null default current_timestamp,
  updated_at datetime not null default current_timestamp on update current_timestamp,

  constraint fk_strava_tokens_user foreign key (user_id) references users(user_id)
) engine=InnoDB;
