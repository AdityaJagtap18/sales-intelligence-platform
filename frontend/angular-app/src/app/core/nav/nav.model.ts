export interface NavItem {
  label: string;
  path: string;
  icon: string;
  badge?: string;
}

export interface NavGroup {
  label: string;
  items: NavItem[];
}
