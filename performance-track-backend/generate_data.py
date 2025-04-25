import pandas as pd
import numpy as np
from datetime import datetime, timedelta
import random

def generate_employee_data(num_employees=15, year=2024, month=3, poor_performer_id=None):
    #
    start_date = datetime(year, month, 1)
    #
    if month == 12:
        next_month = datetime(year + 1, 1, 1)
    else:
        next_month = datetime(year, month + 1, 1)
    #
    num_days = (next_month - start_date).days
    
    dates = []
    current_date = start_date
    while current_date.month == month:
        #
        if current_date.weekday() < 5:  # 0-4 表示周一到周五
            dates.append(current_date)
        current_date += timedelta(days=1)
    
    #
    employee_ids = [f'EMP{str(i).zfill(3)}' for i in range(1, num_employees + 1)]
    names = [
        'John Smith', 'Mary Johnson', 'David Lee', 'Sarah Wilson', 'Michael Brown',
        'Emma Davis', 'James Miller', 'Linda Anderson', 'Robert Taylor', 'Jennifer White',
        'William Moore', 'Elizabeth Clark', 'Thomas Hall', 'Patricia Lewis', 'Richard Wright'
    ][:num_employees]
    departments = [
        'R&D', 'Marketing', 'HR', 'Finance', 'Operations',
        'R&D', 'Marketing', 'HR', 'Finance', 'Operations',
        'R&D', 'Marketing', 'HR', 'Finance', 'Operations'
    ][:num_employees]
    
    #
    employee_performance = {emp_id: {
        'attendance_prob': random.uniform(0.92, 0.99),  #
        'late_prob': random.uniform(0.05, 0.15),        #
        'overtime_prob': random.uniform(0.3, 0.6),      #
        'task_completion_rate': random.uniform(0.8, 1.0) #
    } for emp_id in employee_ids}
    
    #
    if poor_performer_id and poor_performer_id in employee_performance:
        employee_performance[poor_performer_id] = {
            'attendance_prob': random.uniform(0.65, 0.75),  #
            'late_prob': random.uniform(0.4, 0.6),         #
            'overtime_prob': random.uniform(0.05, 0.15),   #
            'task_completion_rate': random.uniform(0.5, 0.65) #
        }
    
    data = []
    for date in dates:
        for emp_id, name, dept in zip(employee_ids, names, departments):
            perf = employee_performance[emp_id]
            
            # 对于差评员工，增加额外的缺勤日期
            is_poor_performer = poor_performer_id and emp_id == poor_performer_id
            
            # 是否出勤（基于个人概率）
            is_present = random.random() < (perf['attendance_prob'] * 0.8 if is_poor_performer and date.day % 3 == 0 else perf['attendance_prob'])
            
            # 任务总数：即使未出勤也有基本任务
            if is_present:
                # 出勤时的任务总数
                if is_poor_performer:
                    total_tasks = random.randint(3, 6)  # 差评员工任务较少
                else:
                    total_tasks = random.randint(4, 10)
                
                # 迟到或早退
                if is_poor_performer:
                    # 差评员工经常迟到/早退，且时间较长
                    late_early_minutes = random.randint(15, 60) if random.random() < perf['late_prob'] else random.randint(0, 15)
                else:
                    late_early_minutes = random.randint(0, 30) if random.random() < perf['late_prob'] else 0
                
                # 加班
                if random.random() < perf['overtime_prob']:
                    overtime_hours = round(random.randint(1, 8) * 0.5, 1)
                    if is_poor_performer:
                        overtime_hours = round(random.randint(0, 2) * 0.5, 1)  # 差评员工很少加班
                else:
                    overtime_hours = 0
                
                # 完成任务数
                if is_poor_performer:
                    # 差评员工的任务完成率较低
                    completion_rate = perf['task_completion_rate'] * random.uniform(0.7, 0.9)
                    min_completed = max(int(total_tasks * 0.5), 1)  # 至少完成50%
                else:
                    completion_rate = perf['task_completion_rate'] * random.uniform(0.95, 1.05)
                    min_completed = max(int(total_tasks * 0.8), 1)  # 至少完成80%
                
                completion_rate = min(completion_rate, 1.0)  # 确保不超过100%
                completed_tasks = random.randint(min_completed, max(min_completed, int(total_tasks * completion_rate)))
                completed_tasks = min(completed_tasks, total_tasks)  # 确保不超过总任务数
            else:
                # 未出勤但仍有基本任务
                if is_poor_performer:
                    # 差评员工缺勤时几乎不处理任务
                    total_tasks = random.randint(1, 2)
                    completed_tasks = random.randint(0, 1)
                else:
                    total_tasks = random.randint(1, 3)
                    completion_rate = perf['task_completion_rate'] * random.uniform(0.7, 0.9)
                    completed_tasks = max(1, int(total_tasks * completion_rate))
                
                late_early_minutes = 0
                overtime_hours = 0
            
            data.append({
                'Date': date.strftime('%Y-%m-%d'),
                'EmployeeID': emp_id,
                'EmployeeName': name,
                'Department': dept,
                'Attendance': 'Y' if is_present else 'N',
                'LateEarlyMinutes': late_early_minutes,
                'OvertimeHours': overtime_hours,
                'TotalTasks': total_tasks,
                'CompletedTasks': completed_tasks
            })
    
    return pd.DataFrame(data)

# 生成2024年6月到2025年3月的数据
# 2024年6月到12月的数据
for month in range(6, 13):
    df = generate_employee_data(num_employees=15, year=2024, month=month)
    df.to_excel(f'EmployeePerformance_2024{str(month).zfill(2)}.xlsx', sheet_name='DailyPerformance', index=False)

# 2025年1月到3月的数据，其中2025年2月EMP005为差评员工，3月EMP011为差评员工
for month in range(1, 4):
    if month == 2:
        poor_performer = 'EMP005'
    elif month == 3:
        poor_performer = 'EMP011'
    else:
        poor_performer = None
        
    df = generate_employee_data(num_employees=15, year=2025, month=month, poor_performer_id=poor_performer)
    df.to_excel(f'EmployeePerformance_2025{str(month).zfill(2)}.xlsx', sheet_name='DailyPerformance', index=False)

print("已生成2024年6月至2025年3月的数据，2025年2月EMP005和3月EMP011为差评员工") 