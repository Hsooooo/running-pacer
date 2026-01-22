# Running Pacer Design Specification

> 러닝 데이터 분석 서비스를 위한 디자인 가이드라인

---

## 1. 현재 상태 분석

### 적용된 기술 스택
| 항목 | 현재 상태 |
|------|----------|
| 템플릿 엔진 | Thymeleaf (`home.html`, `login.html`, `activities.html`) |
| 스타일링 | 인라인 CSS (각 HTML 파일 내 `<style>` 태그) |
| 차트 라이브러리 | Chart.js |
| 폰트 | System Font Stack (`-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto`) |

### 현재 디자인 특징
- Apple-inspired 미니멀 디자인
- 라이트 모드 전용
- 박스 그림자와 라운드 코너 사용
- 제한된 컬러 팔레트

---

## 2. 디자인 시스템

### 2.1 Color Palette

#### Primary Colors
```css
:root {
  /* Brand - Running/Energy Theme */
  --primary: #FC4C02;           /* Strava Orange - 액션 버튼, CTA */
  --primary-hover: #E34402;     /* 호버 상태 */
  --primary-light: #FFF3EE;     /* 배경 틴트 */
  
  /* Secondary - Analytics Theme */
  --secondary: #0071E3;         /* Apple Blue - 링크, 강조 */
  --secondary-hover: #0077ED;
  --secondary-light: #E3F2FD;
}
```

#### Semantic Colors
```css
:root {
  /* Status */
  --success: #34C759;           /* 연결됨, 개선 */
  --success-bg: #E8F5E9;
  --warning: #FF9500;           /* 주의 */
  --warning-bg: #FFF8E1;
  --error: #FF3B30;             /* 오류, 하락 */
  --error-bg: #FFEBEE;
  
  /* Chart Colors */
  --chart-hr: #E91E63;          /* 심박수 라인 */
  --chart-pace: #2196F3;        /* 페이스 라인 */
  --chart-distance: #4CAF50;    /* 거리 바 */
}
```

#### Neutral Colors
```css
:root {
  /* Text */
  --text-primary: #1D1D1F;      /* 메인 텍스트 */
  --text-secondary: #86868B;    /* 보조 텍스트, 레이블 */
  --text-tertiary: #AEAEB2;     /* 비활성 */
  
  /* Background */
  --bg-primary: #FFFFFF;        /* 카드 배경 */
  --bg-secondary: #F5F5F7;      /* 페이지 배경 */
  --bg-tertiary: #F9F9FA;       /* 통계 박스 배경 */
  
  /* Border */
  --border-light: #F5F5F7;      /* 리스트 구분선 */
  --border-default: #D2D2D7;    /* 버튼, 인풋 테두리 */
}
```

---

### 2.2 Typography

#### Font Stack
```css
:root {
  --font-sans: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  --font-mono: 'SF Mono', Monaco, 'Cascadia Code', 'Roboto Mono', Consolas, monospace;
}
```

#### Type Scale
| Token | Size | Weight | Line Height | 용도 |
|-------|------|--------|-------------|------|
| `--text-xs` | 0.75rem (12px) | 400 | 1.4 | 레이블, 상태 배지 |
| `--text-sm` | 0.85rem (14px) | 400 | 1.5 | 보조 정보, 날짜 |
| `--text-base` | 1rem (16px) | 400 | 1.5 | 본문 |
| `--text-lg` | 1.25rem (20px) | 600 | 1.4 | 카드 제목 (h2) |
| `--text-xl` | 1.5rem (24px) | 700 | 1.3 | 통계 값 |
| `--text-2xl` | 1.75rem (28px) | 700 | 1.2 | 인사이트 값 |
| `--text-3xl` | 2rem (32px) | 700 | 1.2 | 페이지 제목 (h1) |

---

### 2.3 Spacing

```css
:root {
  --space-1: 0.25rem;   /* 4px */
  --space-2: 0.5rem;    /* 8px */
  --space-3: 0.75rem;   /* 12px */
  --space-4: 1rem;      /* 16px */
  --space-5: 1.25rem;   /* 20px */
  --space-6: 1.5rem;    /* 24px */
  --space-8: 2rem;      /* 32px */
  --space-10: 2.5rem;   /* 40px */
  --space-12: 3rem;     /* 48px */
}
```

---

### 2.4 Border Radius

```css
:root {
  --radius-sm: 4px;     /* 배지, 태그 */
  --radius-md: 8px;     /* 버튼, 인풋, 통계박스 */
  --radius-lg: 12px;    /* 모달 */
  --radius-xl: 16px;    /* 카드 */
  --radius-full: 9999px; /* 아바타, 원형 버튼 */
}
```

---

### 2.5 Shadows

```css
:root {
  /* Elevation */
  --shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.05);
  --shadow-md: 0 4px 12px rgba(0, 0, 0, 0.05);   /* 카드 기본 */
  --shadow-lg: 0 4px 20px rgba(0, 0, 0, 0.1);    /* 모달 */
  --shadow-xl: 0 8px 30px rgba(0, 0, 0, 0.12);   /* 드롭다운 */
  
  /* Interactive */
  --shadow-focus: 0 0 0 3px rgba(0, 113, 227, 0.3); /* 포커스 링 */
}
```

---

## 3. 컴포넌트 스펙

### 3.1 Buttons

#### Primary Button (CTA)
```css
.btn-primary {
  background: var(--secondary);
  color: white;
  padding: var(--space-2) var(--space-4);   /* 8px 16px */
  border-radius: var(--radius-md);
  font-weight: 500;
  font-size: var(--text-sm);
  transition: background-color 0.2s ease;
}
.btn-primary:hover { background: var(--secondary-hover); }
```

#### Strava Button
```css
.btn-strava {
  background: var(--primary);
  color: white;
  /* Strava 연동에만 사용 */
}
```

#### Outline Button
```css
.btn-outline {
  background: transparent;
  border: 1px solid var(--border-default);
  color: var(--text-primary);
}
.btn-outline:hover { background: var(--bg-secondary); }
```

#### Button Sizes
| Size | Padding | Font Size |
|------|---------|-----------|
| Small | 4px 10px | 0.8rem |
| Medium (default) | 8px 16px | 0.9rem |
| Large | 12px 24px | 1rem |

---

### 3.2 Cards

```css
.card {
  background: var(--bg-primary);
  padding: var(--space-6);                /* 24px */
  border-radius: var(--radius-xl);        /* 16px */
  box-shadow: var(--shadow-md);
  margin-bottom: var(--space-6);
}
```

---

### 3.3 Status Badges

```css
.badge {
  display: inline-block;
  padding: var(--space-1) var(--space-2); /* 4px 8px */
  border-radius: var(--radius-sm);
  font-size: var(--text-xs);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.badge-success { background: var(--success-bg); color: var(--success); }
.badge-error { background: var(--error-bg); color: var(--error); }
.badge-warning { background: var(--warning-bg); color: var(--warning); }
```

---

### 3.4 Stat Boxes (통계 카드)

```css
.stat-box {
  background: var(--bg-tertiary);
  padding: var(--space-4);
  border-radius: var(--radius-md);
  text-align: center;
}
.stat-label {
  font-size: var(--text-xs);
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.stat-value {
  font-size: var(--text-xl);
  font-weight: 700;
  margin-top: var(--space-1);
}
.stat-unit {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  font-weight: 400;
}
```

---

### 3.5 Form Inputs

```css
input, select {
  padding: var(--space-2);                /* 8px */
  border: 1px solid var(--border-default);
  border-radius: var(--radius-md);
  font-family: var(--font-sans);
  font-size: var(--text-base);
  transition: border-color 0.2s, box-shadow 0.2s;
}
input:focus {
  outline: none;
  border-color: var(--secondary);
  box-shadow: var(--shadow-focus);
}
```

---

### 3.6 Tables

```css
table { width: 100%; border-collapse: collapse; }
th {
  text-align: left;
  padding: var(--space-3);
  border-bottom: 1px solid var(--border-default);
  color: var(--text-secondary);
  font-size: var(--text-xs);
  text-transform: uppercase;
}
td {
  padding: var(--space-4) var(--space-3);
  border-bottom: 1px solid var(--border-light);
}
tr:hover td { background: var(--bg-secondary); cursor: pointer; }
```

---

### 3.7 Activity List Items

```css
.activity-item {
  padding: var(--space-3);
  border-bottom: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background 0.2s;
}
.activity-item:hover { background: var(--bg-secondary); }
.activity-item.active {
  background: var(--secondary-light);
  border-color: var(--secondary);
}
```

---

## 4. 레이아웃 가이드

### 4.1 Grid System

```css
/* Dashboard Layout */
.container {
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 350px 1fr;
  gap: var(--space-8);
}

/* Insight Grid (4열) */
.insight-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4);
}

/* Stats Grid (4열) */
.detail-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4);
}
```

### 4.2 Breakpoints (반응형)

> [!IMPORTANT]
> 현재 반응형 미적용. 아래 권장 브레이크포인트 참고.

| Breakpoint | 너비 | 용도 |
|------------|------|------|
| `--bp-sm` | 640px | 모바일 |
| `--bp-md` | 768px | 태블릿 |
| `--bp-lg` | 1024px | 작은 데스크탑 |
| `--bp-xl` | 1280px | 큰 데스크탑 |

---

## 5. 차트 스타일 가이드

### Chart.js 커스터마이징

```javascript
const chartColors = {
  heartRate: {
    border: '#E91E63',
    background: 'rgba(233, 30, 99, 0.1)'
  },
  pace: {
    border: '#2196F3',
    background: 'rgba(33, 150, 243, 0.1)'
  },
  trend: {
    improving: '#34C759',
    declining: '#FF3B30',
    stable: '#FF9500'
  }
};

// 공통 옵션
const chartDefaults = {
  tension: 0.4,           // 곡선 부드럽게
  pointRadius: 0,         // 점 숨김
  borderWidth: 2
};
```

---

## 6. 개선 권장 사항

### 즉시 적용 가능

| 항목 | 현재 | 권장 |
|------|------|------|
| CSS 관리 | 인라인 `<style>` | 외부 CSS 파일 (`styles.css`) 분리 |
| 다크 모드 | ❌ | CSS 변수 기반 테마 전환 지원 |
| 호버 효과 | 기본 | 마이크로 인터랙션 추가 (`transform: translateY(-2px)`) |
| 로딩 상태 | ❌ | 스켈레톤 UI 또는 스피너 |

### 중기 개선

| 항목 | 설명 |
|------|------|
| 반응형 | 모바일/태블릿 레이아웃 추가 |
| 애니메이션 | 페이지 전환, 카드 등장 애니메이션 |
| 아이콘 | Lucide 또는 Heroicons 도입 |
| 토스트 | 성공/에러 알림 시스템 |

---

## 7. 파일 구조 권장안

```
src/main/resources/
├── static/
│   ├── css/
│   │   ├── variables.css      # CSS 변수 정의
│   │   ├── base.css           # 리셋, 타이포그래피
│   │   ├── components.css     # 버튼, 카드, 배지 등
│   │   └── pages/
│   │       ├── home.css
│   │       ├── login.css
│   │       └── activities.css
│   └── js/
│       ├── charts.js          # Chart.js 공통 설정
│       └── api.js             # API 호출 함수
└── templates/
    ├── fragments/
    │   ├── head.html          # 공통 head (CSS 링크)
    │   └── footer.html        # 공통 scripts
    ├── home.html
    ├── login.html
    └── activities.html
```

---

## 8. Quick Reference Card

```
┌─────────────────────────────────────────────────────────┐
│  COLORS                                                 │
├─────────────────────────────────────────────────────────┤
│  Primary:    #FC4C02 (Strava Orange)                    │
│  Secondary:  #0071E3 (Apple Blue)                       │
│  Success:    #34C759    Warning: #FF9500    Error: #FF3B30│
│  Text:       #1D1D1F (primary)  #86868B (secondary)     │
│  Background: #FFFFFF (card)     #F5F5F7 (page)          │
├─────────────────────────────────────────────────────────┤
│  SPACING     4 · 8 · 12 · 16 · 20 · 24 · 32 · 40px      │
│  RADIUS      4 · 8 · 12 · 16px                          │
│  FONT        System Font Stack (-apple-system...)       │
└─────────────────────────────────────────────────────────┘
```
