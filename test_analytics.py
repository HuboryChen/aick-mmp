from playwright.sync_api import sync_playwright

def test_analytics():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        
        # 访问登录页
        print("访问登录页...")
        page.goto('http://localhost:80')
        page.wait_for_load_state('networkidle')
        page.screenshot(path='/tmp/login.png', full_page=True)
        
        # 登录
        print("执行登录...")
        page.fill('input[type="text"]', 'admin')
        page.fill('input[type="password"]', 'admin123')
        page.click('button[type="submit"]')
        page.wait_for_timeout(3000)
        page.screenshot(path='/tmp/after_login.png', full_page=True)
        
        # 导航到数据分析页面
        print("导航到数据分析页面...")
        page.click('text=数据分析')
        page.wait_for_timeout(2000)
        page.screenshot(path='/tmp/analytics.png', full_page=True)
        
        # 检查页面内容
        content = page.content()
        if '数据分析' in content:
            print("✓ 数据分析页面加载成功")
        else:
            print("✗ 数据分析页面加载失败")
        
        # 检查是否有统计卡片
        if '设备使用' in content:
            print("✓ 设备使用统计Tab存在")
        if '带宽统计' in content:
            print("✓ 带宽统计Tab存在")
        if '存储统计' in content:
            print("✓ 存储统计Tab存在")
        if '告警统计' in content:
            print("✓ 告警统计Tab存在")
        if '报表订阅' in content:
            print("✓ 报表订阅Tab存在")
        
        # 截图页面
        print("截图保存完成")
        
        browser.close()
        print("\n测试完成!")

if __name__ == '__main__':
    test_analytics()
