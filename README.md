＃ImageProcess
1.此项目为一个人脸识别

2.开发环境：Ubuntu18.04LTS+eclipse+javaFX

3.PCA算法实现

人脸特征空间也可以称作为特征脸，特征脸就是通过K-L变换将原图像样本多维特征空间投射到一个低维的人脸特征空间上面，但是投射后的低维的人脸特征仍然保持原来图像特征空间的主要特性。主要步骤如下：

（1）创建所有训练样本组成的M×N矩阵trainFaceMat。其中，M为样本个数，N为一个训练样本图像所有像素按列相连的像素值。

（2）计算训练样本的平均值矩阵meanFaceMat，该矩阵为1×N矩阵。

Mat normTrainFaceMat = new Mat(m_matTrainingImage.height(), m_matTrainingImage.width(), CvType.CV_32F);
		float[] trainingImgData = new float[m_matTrainingImage.height() * m_matTrainingImage.width()];
		m_matTrainingImage.get(0, 0, trainingImgData);
		// normTrainFaceMat(i,:)= trainFaceMat(i,:)-meanFaceMat;
		// 计算训练样本的平均值矩阵meanFaceMat

		matMeanVector = new Mat(m_matTrainingImage.height(), 1, CvType.CV_32F);// 按行求平均值得到的矩阵
		matMeanVector.create(1, m_matTrainingImage.cols(), CvType.CV_32F);
		for (int i = 0; i < m_matTrainingImage.cols(); i++) {
			Mat colRange = m_matTrainingImage.colRange(i, i + 1);
			Scalar mean = Core.mean(colRange);
			matMeanVector.put(0, i, mean.val[0]);
		}
		System.out.println("计算训练样本的平均值矩阵meanFaceMat:" + matMeanVector);
		System.out.println("计算训练样本的平均值矩阵meanFaceMat:" + matMeanVector.dump());
（3）计算规格化后的训练样本矩阵normTrainFaceMat，矩阵大小为M×N。计算公式为：

normTrainFaceMat(i,:)= trainFaceMat(i,:)-meanFaceMat
// 计算规格化后的训练样本矩阵normTrainFaceMat，矩阵大小为M×N
		float[] matMeanVectorData = new float[m_matTrainingImage.height() * m_matTrainingImage.width()];
		matMeanVector.get(0, 0, matMeanVectorData);
		for (int i = 0; i < m_matTrainingImage.height(); i++) {

			for (int j = 0; j < m_matTrainingImage.width(); j++) {
				normTrainFaceMat.put(i, j, trainingImgData[i * m_matTrainingImage.width() + j] - matMeanVectorData[i]);
			}
			countNum++;
		}
		System.out.println("计算规格化后的训练样本矩阵normTrainFaceMat:" + normTrainFaceMat);
（4）计算normTrainFaceMat* normTrainFaceMat’矩阵的特征值和特征向量。

其中，矩阵V的列向量为特征向量，D为特征值组成的对角阵。

// 计算normTrainFaceMat* normTrainFaceMat’矩阵的特征值和特征向量。
		Mat EigenValues = new Mat();
		Mat EigenVectors = new Mat();
		Mat EigenMat = new Mat();
		Core.mulTransposed(normTrainFaceMat, EigenMat, false);
		System.out.println("训练样本矩阵normTrainFaceMat及转置矩阵乘积:" + EigenMat);
		// Core.gemm(normTrainFaceMat, normTrainFaceMat.t(), 1.0, new Mat(), 0,
		// EigenMat);
		Core.eigen(EigenMat, EigenValues, EigenVectors);

		System.out.println("EigenValues:" + EigenValues.dump());
		System.out.println("EigenVectors:" + EigenVectors.dump());
（5）将特征值按降序排序，Matlab中的计算公式为：

[sort_value, index] = sort(sum(D), ‘descend’)
其中，sort_value为排序后的特征值，index为排序后的特征值在原序列中的索引。
（6）由于特征向量与特征值是相互对应的，将特征向量依特征值序列也进行排序，Matlab中的计算公式为：

sort_vector = V(:, index)
得到的sort_vector矩阵大小为M×M。
（7）对特征空间进行降维：在sort_value中取占总能量(特征值之和)90%的前m个特征值所对应的特征向量形成新的特征向量eig_vec矩阵，该矩阵大小为M×m，计算方法为：

for i=1:m
eig_vec(:, i) = sort_vector(:, i)/sqrt(sort_value(i)); 
end
		// 对特征空间进行降维

		float[] value = new float[EigenValues.height() * EigenValues.width()];
		EigenValues.get(0, 0, value);

		int m = 0;
		float ValuesSum = 0, sum = 0;
		for (int i = 0; i < EigenValues.rows(); i++) {
			ValuesSum = ValuesSum + value[i];
		}
		for (int i = 0; i < EigenValues.rows(); i++) {
			sum = sum + value[i];
			if (sum / ValuesSum >= 0.9) {
				m = i;
				break;
			}
		}
		m++;
		System.out.println("m=" + m);
		Mat eig_vec = new Mat();// 存放降维后的矩阵
		eig_vec.create(EigenVectors.rows(), m, CvType.CV_32F);
		for (int y = 0; y < m; y++) {
			for (int x = 0; x < EigenVectors.rows(); x++) {
				double a = EigenVectors.get(x, y)[0];
				double b = EigenValues.get(y, 0)[0];
				double c = a / Math.sqrt(b);
				eig_vec.put(x, y, c);

			}
		}
		// System.out.println("eig_vec:" + eig_vec.dump());
（8）获得训练样本的特征脸空间：

eigenface = normTrainFaceMat' * eig_vec
得到的矩阵eigenface大小为N×m，每一列是一个长度为N的特征脸，共m列。
// 训练样本的特征脸空间
		EigenFace = new Mat();
		Core.gemm(normTrainFaceMat.t(), eig_vec, 1.0, new Mat(), 0, EigenFace);
		System.out.println("EigenFace:" + EigenFace);
		System.out.println("EigenFace:" + EigenFace.dump());


4.下载后只需安装即可使用。

5.使用步骤：

a.下载此项目

b.解压到任意目录

c.导入到eclipse中运行即可
