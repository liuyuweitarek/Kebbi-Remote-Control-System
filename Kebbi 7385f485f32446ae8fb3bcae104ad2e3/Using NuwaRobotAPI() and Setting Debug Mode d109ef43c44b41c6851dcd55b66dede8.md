# Using NuwaRobotAPI() and Setting Debug Mode

# 初步想法

1.  需要先熟悉Kebbi的機器人架構：主要包含核心API的調用方式、以及API中的狀態劃分。 
    
    Step1. 從官網的Sample Code下手，凱比先能執行官網釋出的Sample Code。
    
    → 發現Sample Code 是一個List View，每點進一個View都是一個獨立的Activity
    
    Step2. 從官網的Sample Code 定義自己寫的Activity，方便獨立測試自己想要的功能。
    
    → [Steps of Robot Functions Test Based on Sample Code Project](Steps%20of%20Robot%20Functions%20Test%20Based%20on%20Sample%20Code%2018adbb087c0645678e163c4e72b73b5f.md)
    
2. 再更深入了解gRPC協定，主要評估目標為「即時串流資料處理」，以及「重連狀態恢復與管理」。
3. 實作基本架構Android APP(gRPC_Client) 連線至 Python(gRPC_Server)
4. 實作 Python 說話 API
5. 制定初步Server/Client中間協定
6. Zenbo / Kebbi API參數比較 → 再次制定協定

QQ 先做吧