-- Dev-only seed data for incidents and runbooks
-- This file is only executed in the dev profile

INSERT INTO incidents (source, title, description, severity, status, created_at, updated_at)
VALUES 
('cloudwatch', 'Memory Leak Detected - ECS Service api-gateway', 'Alarm Name: ECS-Memory-High\nNamespace: AWS/ECS\nMetric: MemoryUtilization\nThreshold: > 85%', 'HIGH', 'OPEN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('cloudwatch', '5XX Errors Spike - Application Load Balancer', 'Alarm Name: ELB-5XX-Errors\nNamespace: AWS/ELB\nMetric: HTTPCode_ELB_5XX_Count\nThreshold: > 50 per minute', 'CRITICAL', 'OPEN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('manual', 'Slow Page Loads - Checkout Flow', 'Users reporting intermittent slowness during checkout process.', 'MEDIUM', 'OPEN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO runbook_fragments (title, content, tags, created_at)
VALUES
('EC2 High CPU Troubleshooting', '1. Check top processes\n2. Identify high CPU process\n3. Check logs for errors\n4. Restart service if needed', 'cpu,ec2,troubleshooting', CURRENT_TIMESTAMP),
('RDS Connection Pool Management', '1. Check active connections\n2. Identify long running queries\n3. Kill idle transactions\n4. Scale up if necessary', 'database,rds,postgres', CURRENT_TIMESTAMP),
('Network Latency Investigation', '1. Ping gateway\n2. Trace route to target\n3. Check VPC flow logs\n4. Verify security group rules', 'network,latency', CURRENT_TIMESTAMP);
