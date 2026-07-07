import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';
import App from './App.vue';

describe('App', () => {
  it('renders the advisor integration workbench shell', () => {
    const wrapper = mount(App);

    expect(wrapper.text()).toContain('A-Share Advisor');
    expect(wrapper.text()).toContain('生成报告');
    expect(wrapper.text()).toContain('五类 Agent 分析');
    expect(wrapper.text()).toContain('质量分');
    expect(wrapper.text()).toContain('证据链');
    expect(wrapper.text()).toContain('原始 JSON');
    expect(wrapper.text()).toContain('基于当前报告追问');
    expect(wrapper.text()).toContain('下一阶段接入');
  });
});
