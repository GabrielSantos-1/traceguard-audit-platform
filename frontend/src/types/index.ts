export interface KpiData {
  title: string
  value: string
  change: string
  trend: "up" | "down" | "neutral"
  icon: string
  iconBg: string
  iconColor: string
}

export interface SeverityItem {
  label: string
  value: number
  percentage: string
  color: string
}

export interface RecentEvent {
  type: string
  description: string
  time: string
  severity: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL"
  dotColor: string
}

export interface CriticalAlert {
  title: string
  description: string
  severity: "CRITICAL" | "HIGH"
  badgeColor: string
}

export interface TimelineData {
  time: string
  eventos: number
}

export interface NavItem {
  label: string
  icon: string
  active?: boolean
}
