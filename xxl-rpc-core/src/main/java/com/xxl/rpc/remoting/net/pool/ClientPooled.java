package com.xxl.rpc.remoting.net.pool;

import com.xxl.rpc.remoting.invoker.XxlRpcInvokerFactory;
import com.xxl.rpc.remoting.net.params.BaseCallback;
import com.xxl.rpc.remoting.net.params.XxlRpcRequest;
import com.xxl.rpc.serialize.Serializer;
import com.xxl.rpc.util.IpUtil;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author xuxueli 2018-10-19
 */
public abstract class ClientPooled {
    protected static transient Logger logger = LoggerFactory.getLogger(ClientPooled.class);


    // ---------------------- iface ----------------------

    public abstract void init(String host, int port, Serializer serializer) throws Exception;

    public abstract void close();

    public abstract boolean isValidate();

    public abstract void send(XxlRpcRequest xxlRpcRequest) throws Exception ;


    // ---------------------- client pool map ----------------------

    private static ConcurrentHashMap<String, GenericObjectPool<ClientPooled>> clientPoolMap;
    public static GenericObjectPool<ClientPooled> getPool(String address, Serializer serializer, Class<? extends ClientPooled> clientPoolImpl, XxlRpcInvokerFactory xxlRpcInvokerFactory) throws Exception {

        if (clientPoolMap == null) {
            // init
            clientPoolMap = new ConcurrentHashMap<String, GenericObjectPool<ClientPooled>>();
            // stop callback
            xxlRpcInvokerFactory.addStopCallBack(new BaseCallback() {
                @Override
                public void run() throws Exception {
                    if (clientPoolMap.size() > 0) {
                        for (String key:clientPoolMap.keySet()) {
                            GenericObjectPool<ClientPooled> clientPool = clientPoolMap.get(key);
                            clientPool.close();
                        }
                        clientPoolMap.clear();
                    }
                }
            });
        }

        // get pool
        GenericObjectPool<ClientPooled> clientPool = clientPoolMap.get(address);
        if (clientPool != null) {
            return clientPool;
        }

        // parse address
        Object[] array = IpUtil.parseIpPort(address);
        String host = (String) array[0];
        int port = (int) array[1];

        // set pool
        clientPool = new GenericObjectPool<ClientPooled>(new ClientPoolFactory(host, port, serializer, clientPoolImpl));
        clientPool.setTestOnBorrow(true);
        clientPool.setMaxTotal(2);

        clientPoolMap.put(address, clientPool);

        return clientPool;
    }

}
