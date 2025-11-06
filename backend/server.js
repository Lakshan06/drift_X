const WebSocket = require('ws');
const express = require('express');
const { v4: uuidv4 } = require('uuid');

// Configuration
const PORT = process.env.PORT || 8080;
const DRIFT_CHECK_INTERVAL = 15000; // Check for drift every 15 seconds
const TELEMETRY_INTERVAL = 5000; // Send telemetry every 5 seconds

// Express app for health checks
const app = express();
app.use(express.json());

app.get('/health', (req, res) => {
  res.json({ 
    status: 'healthy', 
    timestamp: Date.now(),
    connections: wss.clients.size 
  });
});

// Start HTTP server
const server = app.listen(PORT, () => {
  console.log(`🚀 DriftGuard Backend running on port ${PORT}`);
  console.log(`📡 WebSocket server ready for connections`);
  console.log(`🔗 Connect to: ws://localhost:${PORT}`);
});

// Create WebSocket server
const wss = new WebSocket.Server({ server });

// Store connected clients and their subscriptions
const clients = new Map();
const modelSubscriptions = new Map();

// Simulated deployed models
const deployedModels = new Map([
  ['fraud-detector-v1', { 
    name: 'Fraud Detector v1', 
    version: '1.0', 
    status: 'deployed',
    lastCheck: Date.now()
  }],
  ['churn-predictor-v2', { 
    name: 'Churn Predictor v2', 
    version: '2.0', 
    status: 'deployed',
    lastCheck: Date.now()
  }],
  ['credit-scorer-v1', { 
    name: 'Credit Scorer v1', 
    version: '1.0', 
    status: 'deployed',
    lastCheck: Date.now()
  }]
]);

// WebSocket connection handler
wss.on('connection', (ws, req) => {
  const clientId = uuidv4();
  const clientInfo = {
    id: clientId,
    ws: ws,
    authenticated: false,
    subscriptions: new Set(),
    connectedAt: Date.now()
  };
  
  clients.set(clientId, clientInfo);
  
  console.log(`\n✅ New client connected: ${clientId}`);
  console.log(`📊 Total connections: ${clients.size}`);

  // Send welcome message
  sendMessage(ws, {
    type: 'welcome',
    message: 'Connected to DriftGuard Monitoring Server',
    server_time: Date.now()
  });

  // Handle messages from client
  ws.on('message', (data) => {
    try {
      const message = JSON.parse(data);
      handleClientMessage(clientId, message);
    } catch (error) {
      console.error(`❌ Error parsing message:`, error.message);
      sendMessage(ws, {
        type: 'error',
        message: 'Invalid JSON message'
      });
    }
  });

  // Handle client disconnect
  ws.on('close', () => {
    console.log(`\n⏹️  Client disconnected: ${clientId}`);
    
    // Clean up subscriptions
    const client = clients.get(clientId);
    if (client) {
      client.subscriptions.forEach(modelId => {
        unsubscribeClientFromModel(clientId, modelId);
      });
    }
    
    clients.delete(clientId);
    console.log(`📊 Total connections: ${clients.size}`);
  });

  ws.on('error', (error) => {
    console.error(`❌ WebSocket error for ${clientId}:`, error.message);
  });

  // Send initial telemetry after connection
  setTimeout(() => {
    if (ws.readyState === WebSocket.OPEN) {
      sendInitialData(ws);
    }
  }, 1000);
});

// Handle different message types from client
function handleClientMessage(clientId, message) {
  const client = clients.get(clientId);
  if (!client) return;

  const { type, ...data } = message;

  console.log(`\n📩 Message from ${clientId}: ${type}`);

  switch (type) {
    case 'auth':
      handleAuth(client, data);
      break;

    case 'subscribe':
      handleSubscribe(client, data);
      break;

    case 'unsubscribe':
      handleUnsubscribe(client, data);
      break;

    case 'status_request':
      handleStatusRequest(client, data);
      break;

    case 'patch_command':
      handlePatchCommand(client, data);
      break;

    case 'ping':
      sendMessage(client.ws, { type: 'pong', timestamp: Date.now() });
      break;

    case 'telemetry':
      handleTelemetry(client, data);
      break;

    default:
      console.log(`⚠️  Unknown message type: ${type}`);
      sendMessage(client.ws, {
        type: 'error',
        message: `Unknown message type: ${type}`
      });
  }
}

// Authentication handler (simple demo version)
function handleAuth(client, data) {
  const { token } = data;
  
  // Simple authentication - accept any token for demo
  if (token) {
    client.authenticated = true;
    sendMessage(client.ws, {
      type: 'auth_success',
      message: 'Authentication successful'
    });
    console.log(`✅ Client ${client.id} authenticated`);
  } else {
    sendMessage(client.ws, {
      type: 'auth_failed',
      message: 'Invalid or missing token'
    });
    console.log(`❌ Client ${client.id} authentication failed`);
  }
}

// Subscribe to model monitoring
function handleSubscribe(client, data) {
  const { modelId } = data;
  
  if (!modelId) {
    sendMessage(client.ws, {
      type: 'error',
      message: 'Missing modelId'
    });
    return;
  }

  // Add subscription
  client.subscriptions.add(modelId);
  
  // Track model subscribers
  if (!modelSubscriptions.has(modelId)) {
    modelSubscriptions.set(modelId, new Set());
  }
  modelSubscriptions.get(modelId).add(client.id);

  sendMessage(client.ws, {
    type: 'subscription_confirmed',
    modelId: modelId,
    message: `Subscribed to model: ${modelId}`
  });

  console.log(`📡 Client ${client.id} subscribed to model: ${modelId}`);

  // Send initial model status
  sendModelStatus(client, modelId);
}

// Unsubscribe from model monitoring
function handleUnsubscribe(client, data) {
  const { modelId } = data;
  
  unsubscribeClientFromModel(client.id, modelId);
  
  sendMessage(client.ws, {
    type: 'unsubscription_confirmed',
    modelId: modelId
  });

  console.log(`📡 Client ${client.id} unsubscribed from model: ${modelId}`);
}

function unsubscribeClientFromModel(clientId, modelId) {
  const client = clients.get(clientId);
  if (client) {
    client.subscriptions.delete(modelId);
  }
  
  const subscribers = modelSubscriptions.get(modelId);
  if (subscribers) {
    subscribers.delete(clientId);
    if (subscribers.size === 0) {
      modelSubscriptions.delete(modelId);
    }
  }
}

// Handle status request
function handleStatusRequest(client, data) {
  const { modelId } = data;
  sendModelStatus(client, modelId);
}

// Handle patch deployment command
function handlePatchCommand(client, data) {
  const { modelId, patchId, action } = data;
  
  console.log(`🚀 Patch command received: ${action} patch ${patchId} for model ${modelId}`);

  // Simulate patch deployment
  setTimeout(() => {
    sendMessage(client.ws, {
      type: 'patch_status',
      modelId: modelId,
      patchId: patchId,
      status: 'deploying',
      message: 'Patch deployment started'
    });

    // Simulate deployment progress
    setTimeout(() => {
      sendMessage(client.ws, {
        type: 'patch_status',
        modelId: modelId,
        patchId: patchId,
        status: 'deployed',
        message: 'Patch deployed successfully'
      });

      // Send drift alert showing improvement
      setTimeout(() => {
        broadcastDriftAlert(modelId, {
          severity: 'low',
          driftScore: 0.15,
          message: 'Drift reduced after patch deployment'
        });
      }, 2000);

    }, 3000);
  }, 1000);
}

// Handle telemetry from deployed models
function handleTelemetry(client, data) {
  console.log(`📊 Telemetry received:`, data);
  
  // Broadcast telemetry to subscribers
  if (data.data && data.data.modelId) {
    broadcastToSubscribers(data.data.modelId, {
      type: 'telemetry',
      data: data.data
    });
  }
}

// Send model status
function sendModelStatus(client, modelId) {
  const model = deployedModels.get(modelId);
  
  if (model) {
    sendMessage(client.ws, {
      type: 'status_update',
      modelId: modelId,
      status: model.status,
      name: model.name,
      version: model.version,
      lastCheck: model.lastCheck,
      metrics: {
        requests_per_second: Math.random() * 100,
        avg_latency_ms: Math.random() * 50 + 10,
        error_rate: Math.random() * 0.01,
        drift_score: Math.random() * 0.5
      }
    });
  } else {
    sendMessage(client.ws, {
      type: 'error',
      message: `Model not found: ${modelId}`
    });
  }
}

// Send initial data to new client
function sendInitialData(ws) {
  // Send list of deployed models
  sendMessage(ws, {
    type: 'models_list',
    models: Array.from(deployedModels.entries()).map(([id, model]) => ({
      id,
      ...model
    }))
  });
}

// Broadcast message to all subscribers of a model
function broadcastToSubscribers(modelId, message) {
  const subscribers = modelSubscriptions.get(modelId);
  if (!subscribers) return;

  subscribers.forEach(clientId => {
    const client = clients.get(clientId);
    if (client && client.ws.readyState === WebSocket.OPEN) {
      sendMessage(client.ws, message);
    }
  });
}

// Broadcast drift alert
function broadcastDriftAlert(modelId, alertData) {
  const alert = {
    type: 'drift_alert',
    data: {
      modelId: modelId,
      timestamp: Date.now(),
      severity: alertData.severity,
      driftScore: alertData.driftScore,
      driftType: alertData.driftType || 'distribution_shift',
      affectedFeatures: alertData.affectedFeatures || ['feature_0', 'feature_3', 'feature_7'],
      message: alertData.message,
      recommendedAction: alertData.recommendedAction || 'Consider retraining or applying a patch'
    }
  };

  broadcastToSubscribers(modelId, alert);
  console.log(`⚠️  Drift alert broadcast for ${modelId}: ${alertData.severity}`);
}

// Send telemetry event
function broadcastTelemetry(modelId) {
  const telemetry = {
    type: 'telemetry',
    data: {
      modelId: modelId,
      timestamp: Date.now(),
      inputFeatures: {
        'feature_0': Math.random(),
        'feature_1': Math.random() * 10,
        'feature_2': Math.random() * 100,
        'feature_3': Math.random() * 5
      },
      prediction: Math.random(),
      confidence: 0.7 + Math.random() * 0.3,
      latency: Math.floor(Math.random() * 50) + 10,
      metadata: {
        request_id: uuidv4(),
        client_id: 'production-api-1'
      }
    }
  };

  broadcastToSubscribers(modelId, telemetry);
}

// Helper to send message
function sendMessage(ws, message) {
  if (ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify(message));
  }
}

// ============================================================================
// SIMULATION ENGINE - Demonstrates deployment monitoring in action
// ============================================================================

// Periodically check for drift and send alerts
setInterval(() => {
  // Only check models that have subscribers
  modelSubscriptions.forEach((subscribers, modelId) => {
    if (subscribers.size > 0) {
      // Randomly decide if drift occurs (30% chance)
      if (Math.random() < 0.3) {
        const driftScore = Math.random();
        let severity = 'low';
        
        if (driftScore > 0.7) severity = 'critical';
        else if (driftScore > 0.5) severity = 'high';
        else if (driftScore > 0.3) severity = 'medium';

        broadcastDriftAlert(modelId, {
          severity: severity,
          driftScore: driftScore,
          driftType: ['distribution_shift', 'concept_drift', 'feature_drift'][Math.floor(Math.random() * 3)],
          message: `${severity.toUpperCase()} drift detected in production`,
          affectedFeatures: [
            `feature_${Math.floor(Math.random() * 10)}`,
            `feature_${Math.floor(Math.random() * 10)}`
          ]
        });
      }
    }
  });
}, DRIFT_CHECK_INTERVAL);

// Periodically send telemetry for monitored models
setInterval(() => {
  modelSubscriptions.forEach((subscribers, modelId) => {
    if (subscribers.size > 0) {
      broadcastTelemetry(modelId);
    }
  });
}, TELEMETRY_INTERVAL);

// Update model statistics
setInterval(() => {
  deployedModels.forEach((model, modelId) => {
    model.lastCheck = Date.now();
  });
}, 30000);

// Graceful shutdown
process.on('SIGINT', () => {
  console.log('\n\n🛑 Shutting down server...');
  
  // Notify all clients
  clients.forEach(client => {
    sendMessage(client.ws, {
      type: 'server_shutdown',
      message: 'Server is shutting down'
    });
    client.ws.close();
  });

  wss.close(() => {
    server.close(() => {
      console.log('✅ Server shut down gracefully');
      process.exit(0);
    });
  });
});

console.log('\n');
console.log('='.repeat(60));
console.log('  🚀 DriftGuard Deployment Monitoring Server');
console.log('='.repeat(60));
console.log(`  📡 WebSocket: ws://localhost:${PORT}`);
console.log(`  🏥 Health: http://localhost:${PORT}/health`);
console.log(`  📊 Monitoring ${deployedModels.size} deployed models`);
console.log('='.repeat(60));
console.log('\n');
